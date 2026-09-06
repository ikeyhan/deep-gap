from __future__ import annotations

from pydantic import BaseModel, Field


class ImageGenerateRequest(BaseModel):
    prompt: str = Field(min_length=1, max_length=2000)
    model_code: str = "designer"
    size: str = "1024x1024"
    n: int = Field(default=1, ge=1, le=4)


class ImageGenerateResponse(BaseModel):
    urls: list[str]
    model_code: str
    charged_credit: float
    balance: float
