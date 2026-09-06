from __future__ import annotations

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class ConversationCreate(BaseModel):
    title: str | None = None
    model_code: str | None = None


class ConversationOut(BaseModel):
    id: uuid.UUID
    title: str
    model_code: str | None
    is_pinned: bool
    is_favorite: bool
    is_archived: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class MessageOut(BaseModel):
    id: uuid.UUID
    role: str
    content: str
    model_code: str | None
    status: str
    created_at: datetime

    class Config:
        from_attributes = True


class SendMessageRequest(BaseModel):
    content: str = Field(min_length=1, max_length=32000)
    model_code: str = "fast"
    stream: bool = False


class SendMessageResponse(BaseModel):
    conversation_id: uuid.UUID
    user_message: MessageOut
    assistant_message: MessageOut
    charged_credit: float
    balance: float
