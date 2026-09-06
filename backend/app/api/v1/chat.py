from __future__ import annotations

import json
import time
import uuid

from fastapi import APIRouter, Query
from fastapi.responses import StreamingResponse
from sqlalchemy import select

from app.core.deps import CurrentUser, DbDep
from app.core.exceptions import InsufficientCreditError, NotFoundError, ProviderError
from app.core.logging import get_logger
from app.models.ai import ModelPricing
from app.models.chat import Conversation, Message
from app.schemas.chat import (
    ConversationCreate,
    ConversationOut,
    MessageOut,
    SendMessageRequest,
    SendMessageResponse,
)
from app.services.ai.base import ChatMessage, TextRequest
from app.services.ai.router import AIRouter
from app.services.auth_service import user_tier
from app.services.billing_service import BillingService
from app.services.wallet_service import WalletService

router = APIRouter(prefix="/conversations", tags=["chat"])
log = get_logger("chat")

_HISTORY_LIMIT = 20


async def _get_conversation(db, user_id: uuid.UUID, conversation_id: uuid.UUID) -> Conversation:
    conv = (
        await db.execute(
            select(Conversation).where(
                Conversation.id == conversation_id,
                Conversation.user_id == user_id,
                Conversation.deleted_at.is_(None),
            )
        )
    ).scalar_one_or_none()
    if conv is None:
        raise NotFoundError("گفت‌وگو یافت نشد", error_code="conversation_not_found")
    return conv


async def _history(db, conversation_id: uuid.UUID) -> list[ChatMessage]:
    stmt = (
        select(Message)
        .where(Message.conversation_id == conversation_id, Message.deleted_at.is_(None))
        .order_by(Message.created_at.desc())
        .limit(_HISTORY_LIMIT)
    )
    rows = list((await db.execute(stmt)).scalars().all())
    rows.reverse()
    return [ChatMessage(role=m.role, content=m.content) for m in rows]


# ---------------------------------------------------------------- conversations


@router.post("", response_model=ConversationOut, status_code=201)
async def create_conversation(payload: ConversationCreate, user: CurrentUser, db: DbDep):
    conv = Conversation(
        user_id=user.id,
        title=payload.title or "گفت‌وگوی جدید",
        model_code=payload.model_code,
    )
    db.add(conv)
    await db.commit()
    await db.refresh(conv)
    return ConversationOut.model_validate(conv)


@router.get("", response_model=list[ConversationOut])
async def list_conversations(user: CurrentUser, db: DbDep, limit: int = 50, offset: int = 0):
    stmt = (
        select(Conversation)
        .where(Conversation.user_id == user.id, Conversation.deleted_at.is_(None))
        .order_by(Conversation.is_pinned.desc(), Conversation.updated_at.desc())
        .limit(min(limit, 100))
        .offset(offset)
    )
    rows = (await db.execute(stmt)).scalars().all()
    return [ConversationOut.model_validate(c) for c in rows]


@router.get("/{conversation_id}/messages", response_model=list[MessageOut])
async def list_messages(conversation_id: uuid.UUID, user: CurrentUser, db: DbDep):
    await _get_conversation(db, user.id, conversation_id)
    stmt = (
        select(Message)
        .where(Message.conversation_id == conversation_id, Message.deleted_at.is_(None))
        .order_by(Message.created_at.asc())
    )
    rows = (await db.execute(stmt)).scalars().all()
    return [MessageOut.model_validate(m) for m in rows]


@router.delete("/{conversation_id}")
async def delete_conversation(conversation_id: uuid.UUID, user: CurrentUser, db: DbDep):
    from datetime import datetime

    conv = await _get_conversation(db, user.id, conversation_id)
    conv.deleted_at = datetime.utcnow()
    await db.commit()
    return {"message": "گفت‌وگو حذف شد"}


# ---------------------------------------------------------------- messaging


async def _preflight(db, user, request_model_code: str):
    """Resolve model + pricing and verify the user can afford it (spec rule 13)."""
    tier = await user_tier(db, user)
    ai_router = AIRouter(db)
    resolved = await ai_router.resolve(request_model_code, user_tier=tier)

    pricing = (
        await db.execute(
            select(ModelPricing).where(ModelPricing.model_id == resolved.model.id)
        )
    ).scalar_one_or_none()
    if pricing is None:
        raise ProviderError("قیمت مدل تعریف نشده است", error_code="pricing_missing")

    billing = BillingService(db)
    wallet = await WalletService(db).get_or_create(user.id)
    est = billing.estimate_max_charge(resolved.model, pricing)
    if wallet.balance < est:
        raise InsufficientCreditError("اعتبار کافی برای این درخواست ندارید")
    return resolved, pricing, billing


@router.post("/{conversation_id}/messages", response_model=SendMessageResponse)
async def send_message(
    conversation_id: uuid.UUID,
    payload: SendMessageRequest,
    user: CurrentUser,
    db: DbDep,
):
    conv = await _get_conversation(db, user.id, conversation_id)
    resolved, pricing, billing = await _preflight(db, user, payload.model_code)

    # Persist the user's message first.
    user_msg = Message(
        conversation_id=conv.id, role="user", content=payload.content, model_code=payload.model_code
    )
    db.add(user_msg)
    await db.flush()

    history = await _history(db, conv.id)
    history.append(ChatMessage(role="user", content=payload.content))

    request_id = str(uuid.uuid4())
    usage_log = await billing.open_usage(
        user_id=user.id,
        model=resolved.model,
        request_type="chat",
        request_id=request_id,
    )

    started = time.perf_counter()
    try:
        result = await resolved.adapter.generate_text(
            TextRequest(
                model=resolved.model.provider_model,
                messages=history,
                max_tokens=resolved.model.max_tokens,
                request_id=request_id,
            )
        )
    except Exception as exc:  # noqa: BLE001
        await billing.fail_usage(log=usage_log, reason=str(exc))
        await db.commit()
        log.warning("chat_provider_failed", request_id=request_id, error=str(exc))
        raise ProviderError("خطا در ارتباط با سرویس هوش مصنوعی") from exc

    latency_ms = int((time.perf_counter() - started) * 1000)
    charge = await billing.settle_usage(
        log=usage_log, pricing=pricing, usage=result.usage, latency_ms=latency_ms
    )

    assistant_msg = Message(
        conversation_id=conv.id,
        role="assistant",
        content=result.content,
        model_code=payload.model_code,
        input_tokens=result.usage.input_tokens,
        output_tokens=result.usage.output_tokens,
    )
    db.add(assistant_msg)

    wallet = await WalletService(db).get_or_create(user.id)
    await db.commit()
    await db.refresh(user_msg)
    await db.refresh(assistant_msg)

    return SendMessageResponse(
        conversation_id=conv.id,
        user_message=MessageOut.model_validate(user_msg),
        assistant_message=MessageOut.model_validate(assistant_msg),
        charged_credit=float(charge),
        balance=float(wallet.balance),
    )


@router.post("/{conversation_id}/messages/stream")
async def stream_message(
    conversation_id: uuid.UUID,
    payload: SendMessageRequest,
    user: CurrentUser,
    db: DbDep,
):
    """Server-Sent Events streaming. Billing settles AFTER the stream ends so
    streamed output is never free (spec rule 13)."""
    conv = await _get_conversation(db, user.id, conversation_id)
    resolved, pricing, billing = await _preflight(db, user, payload.model_code)

    user_msg = Message(
        conversation_id=conv.id, role="user", content=payload.content, model_code=payload.model_code
    )
    db.add(user_msg)
    await db.flush()
    history = await _history(db, conv.id)
    history.append(ChatMessage(role="user", content=payload.content))
    request_id = str(uuid.uuid4())
    usage_log = await billing.open_usage(
        user_id=user.id, model=resolved.model, request_type="chat", request_id=request_id
    )
    await db.commit()

    async def event_stream():
        started = time.perf_counter()
        collected: list[str] = []
        try:
            async for chunk in resolved.adapter.stream_text(
                TextRequest(
                    model=resolved.model.provider_model,
                    messages=history,
                    max_tokens=resolved.model.max_tokens,
                    stream=True,
                    request_id=request_id,
                )
            ):
                if chunk.delta:
                    collected.append(chunk.delta)
                    yield f"data: {json.dumps({'delta': chunk.delta}, ensure_ascii=False)}\n\n"
                if chunk.finished:
                    break
        except Exception as exc:  # noqa: BLE001
            await billing.fail_usage(log=usage_log, reason=str(exc))
            await db.commit()
            yield f"event: error\ndata: {json.dumps({'message': 'provider_error'})}\n\n"
            return

        content = "".join(collected)
        latency_ms = int((time.perf_counter() - started) * 1000)

        from app.services.ai.base import Usage

        usage = Usage(
            input_tokens=sum(len(m.content) // 4 for m in history),
            output_tokens=max(1, len(content) // 4),
        )
        charge = await billing.settle_usage(
            log=usage_log, pricing=pricing, usage=usage, latency_ms=latency_ms
        )
        db.add(
            Message(
                conversation_id=conv.id,
                role="assistant",
                content=content,
                model_code=payload.model_code,
                output_tokens=usage.output_tokens,
            )
        )
        await db.commit()
        yield f"event: done\ndata: {json.dumps({'charged_credit': float(charge)})}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@router.get("/{conversation_id}", response_model=ConversationOut)
async def get_conversation(
    conversation_id: uuid.UUID, user: CurrentUser, db: DbDep, _q: str | None = Query(None)
):
    conv = await _get_conversation(db, user.id, conversation_id)
    return ConversationOut.model_validate(conv)
