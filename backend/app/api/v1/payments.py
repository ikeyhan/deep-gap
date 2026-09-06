from __future__ import annotations

import uuid

from fastapi import APIRouter, Request

from app.core.deps import CurrentUser, DbDep
from app.core.exceptions import NotFoundError
from app.models.platform import Payment
from app.schemas.billing import (
    PaymentInitiateRequest,
    PaymentInitiateResponse,
    PaymentStatusOut,
)
from app.services.payment_service import PaymentService

router = APIRouter(prefix="/payments", tags=["payments"])


@router.post("/initiate", response_model=PaymentInitiateResponse)
async def initiate(payload: PaymentInitiateRequest, user: CurrentUser, db: DbDep):
    service = PaymentService(db)
    payment, url = await service.initiate(
        user_id=user.id, product_type=payload.product_type, code=payload.code
    )
    await db.commit()
    return PaymentInitiateResponse(
        payment_id=payment.id, status=payment.status, amount=payment.amount, payment_url=url
    )


@router.api_route("/callback", methods=["GET", "POST"], response_model=PaymentStatusOut)
async def callback(request: Request, db: DbDep):
    """Gateway redirect target. Verification is server-side and authoritative.

    A client cannot fake success: verify recomputes/queries the gateway proof.
    """
    params = dict(request.query_params)
    if request.method == "POST":
        try:
            body = await request.json()
            if isinstance(body, dict):
                params.update(body)
        except Exception:  # noqa: BLE001
            pass

    payment_id = params.get("payment_id")
    if not payment_id:
        raise NotFoundError("شناسهٔ پرداخت موجود نیست", error_code="payment_id_missing")

    service = PaymentService(db)
    payment = await service.verify(payment_id=uuid.UUID(payment_id), params=params)
    await db.commit()
    return PaymentStatusOut.model_validate(payment)


@router.post("/{payment_id}/verify", response_model=PaymentStatusOut)
async def verify_receipt(payment_id: uuid.UUID, user: CurrentUser, db: DbDep, request: Request):
    """Server-side verification for in-app-purchase receipts / manual re-check."""
    params = dict(request.query_params)
    try:
        body = await request.json()
        if isinstance(body, dict):
            params.update(body)
    except Exception:  # noqa: BLE001
        pass

    payment = await db.get(Payment, payment_id)
    if payment is None or payment.user_id != user.id:
        raise NotFoundError("پرداخت یافت نشد", error_code="payment_not_found")

    service = PaymentService(db)
    payment = await service.verify(payment_id=payment_id, params=params)
    await db.commit()
    return PaymentStatusOut.model_validate(payment)


@router.get("/{payment_id}", response_model=PaymentStatusOut)
async def get_payment(payment_id: uuid.UUID, user: CurrentUser, db: DbDep):
    payment = await db.get(Payment, payment_id)
    if payment is None or payment.user_id != user.id:
        raise NotFoundError("پرداخت یافت نشد", error_code="payment_not_found")
    return PaymentStatusOut.model_validate(payment)
