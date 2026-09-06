"""Import all models so Alembic autogenerate and metadata see them."""
from app.models.ai import AIModel, AIProviderRow, ModelPricing, UsageLog
from app.models.chat import Conversation, Message, MessageAttachment
from app.models.platform import (
    AdminUser,
    AuditLog,
    FeatureFlag,
    Payment,
    Referral,
    RemoteConfig,
)
from app.models.subscription import CreditPackage, Subscription, SubscriptionPlan
from app.models.user import Device, Session, User, UserProfile
from app.models.wallet import Wallet, WalletTransaction

__all__ = [
    "AIModel",
    "AIProviderRow",
    "ModelPricing",
    "UsageLog",
    "Conversation",
    "Message",
    "MessageAttachment",
    "AdminUser",
    "AuditLog",
    "FeatureFlag",
    "Payment",
    "Referral",
    "RemoteConfig",
    "CreditPackage",
    "Subscription",
    "SubscriptionPlan",
    "Device",
    "Session",
    "User",
    "UserProfile",
    "Wallet",
    "WalletTransaction",
]
