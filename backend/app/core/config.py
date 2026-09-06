"""Application configuration loaded from environment variables."""
from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    # ---- App ----
    app_name: str = Field(default="DeepGap", alias="APP_NAME")
    app_env: str = Field(default="development", alias="APP_ENV")
    app_debug: bool = Field(default=True, alias="APP_DEBUG")
    api_v1_prefix: str = Field(default="/api/v1", alias="API_V1_PREFIX")
    secret_key: str = Field(default="change-me", alias="SECRET_KEY")

    # ---- Database ----
    database_url: str = Field(
        default="postgresql+asyncpg://deepgap:deepgap_dev_password@localhost:5432/deepgap",
        alias="DATABASE_URL",
    )

    # ---- Redis ----
    redis_url: str = Field(default="redis://localhost:6379/0", alias="REDIS_URL")

    # ---- JWT ----
    jwt_secret: str = Field(default="change-me-jwt", alias="JWT_SECRET")
    jwt_algorithm: str = Field(default="HS256", alias="JWT_ALGORITHM")
    access_token_expire_minutes: int = Field(default=30, alias="ACCESS_TOKEN_EXPIRE_MINUTES")
    refresh_token_expire_days: int = Field(default=30, alias="REFRESH_TOKEN_EXPIRE_DAYS")

    # ---- OTP ----
    otp_length: int = Field(default=5, alias="OTP_LENGTH")
    otp_ttl_seconds: int = Field(default=120, alias="OTP_TTL_SECONDS")
    otp_max_attempts: int = Field(default=5, alias="OTP_MAX_ATTEMPTS")
    otp_resend_cooldown_seconds: int = Field(default=60, alias="OTP_RESEND_COOLDOWN_SECONDS")
    otp_provider: str = Field(default="console", alias="OTP_PROVIDER")

    # ---- Object storage ----
    s3_endpoint: str = Field(default="http://localhost:9000", alias="S3_ENDPOINT")
    s3_public_endpoint: str = Field(default="http://localhost:9000", alias="S3_PUBLIC_ENDPOINT")
    s3_access_key: str = Field(default="deepgap", alias="S3_ACCESS_KEY")
    s3_secret_key: str = Field(default="deepgap_dev_password", alias="S3_SECRET_KEY")
    s3_bucket: str = Field(default="deepgap", alias="S3_BUCKET")
    s3_region: str = Field(default="us-east-1", alias="S3_REGION")

    # ---- AI providers ----
    openai_api_key: str = Field(default="", alias="OPENAI_API_KEY")
    openai_base_url: str = Field(default="https://api.openai.com/v1", alias="OPENAI_BASE_URL")
    anthropic_api_key: str = Field(default="", alias="ANTHROPIC_API_KEY")
    google_api_key: str = Field(default="", alias="GOOGLE_API_KEY")

    # ---- Payments ----
    payment_provider: str = Field(default="mock", alias="PAYMENT_PROVIDER")
    payment_callback_url: str = Field(
        default="http://localhost:8000/api/v1/payments/callback", alias="PAYMENT_CALLBACK_URL"
    )

    # ---- Monitoring ----
    sentry_dsn: str = Field(default="", alias="SENTRY_DSN")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    log_json: bool = Field(default=True, alias="LOG_JSON")

    # ---- CORS ----
    cors_origins: str = Field(default="http://localhost:3000", alias="CORS_ORIGINS")

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    @property
    def is_production(self) -> bool:
        return self.app_env == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
