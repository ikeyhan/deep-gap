-- Deep Gap — reference schema (PostgreSQL)
-- Generated from SQLAlchemy models. Source of truth is Alembic migrations.

CREATE TABLE admin_users (
	email VARCHAR(255) NOT NULL, 
	password_hash VARCHAR(255) NOT NULL, 
	role VARCHAR(32) NOT NULL, 
	is_active BOOLEAN NOT NULL, 
	totp_secret VARCHAR(64), 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ix_admin_users_email ON admin_users (email);

CREATE TABLE ai_providers (
	key VARCHAR(64) NOT NULL, 
	name VARCHAR(120) NOT NULL, 
	is_enabled BOOLEAN NOT NULL, 
	priority INTEGER NOT NULL, 
	config JSONB, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	deleted_at TIMESTAMP WITH TIME ZONE, 
	PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ix_ai_providers_key ON ai_providers (key);

CREATE TABLE audit_logs (
	actor_type VARCHAR(16) NOT NULL, 
	actor_id UUID, 
	action VARCHAR(64) NOT NULL, 
	target_type VARCHAR(64), 
	target_id VARCHAR(128), 
	meta JSONB, 
	ip_address VARCHAR(64), 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id)
);


CREATE TABLE credit_packages (
	code VARCHAR(32) NOT NULL, 
	name VARCHAR(120) NOT NULL, 
	price NUMERIC(18, 2) NOT NULL, 
	credit_amount NUMERIC(18, 2) NOT NULL, 
	bonus_credit NUMERIC(18, 2) NOT NULL, 
	is_active BOOLEAN NOT NULL, 
	display_order INTEGER NOT NULL, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	UNIQUE (code)
);


CREATE TABLE feature_flags (
	key VARCHAR(64) NOT NULL, 
	enabled BOOLEAN NOT NULL, 
	rules JSONB, 
	description TEXT, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ix_feature_flags_key ON feature_flags (key);

CREATE TABLE remote_config (
	key VARCHAR(64) NOT NULL, 
	value JSONB, 
	description TEXT, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ix_remote_config_key ON remote_config (key);

CREATE TABLE subscription_plans (
	code VARCHAR(32) NOT NULL, 
	name VARCHAR(120) NOT NULL, 
	price NUMERIC(18, 2) NOT NULL, 
	duration_days INTEGER NOT NULL, 
	monthly_credit NUMERIC(18, 2) NOT NULL, 
	is_active BOOLEAN NOT NULL, 
	features JSONB, 
	display_order INTEGER NOT NULL, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	UNIQUE (code)
);


CREATE TABLE users (
	phone VARCHAR(20) NOT NULL, 
	email VARCHAR(255), 
	is_active BOOLEAN NOT NULL, 
	is_blocked BOOLEAN NOT NULL, 
	is_phone_verified BOOLEAN NOT NULL, 
	referral_code VARCHAR(16) NOT NULL, 
	referred_by UUID, 
	last_login_at TIMESTAMP WITH TIME ZONE, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	deleted_at TIMESTAMP WITH TIME ZONE, 
	PRIMARY KEY (id), 
	FOREIGN KEY(referred_by) REFERENCES users (id)
);

CREATE UNIQUE INDEX ix_users_email ON users (email);
CREATE UNIQUE INDEX ix_users_referral_code ON users (referral_code);
CREATE UNIQUE INDEX ix_users_phone ON users (phone);

CREATE TABLE ai_models (
	provider_id UUID NOT NULL, 
	provider_model VARCHAR(120) NOT NULL, 
	code VARCHAR(64) NOT NULL, 
	display_name VARCHAR(120) NOT NULL, 
	description TEXT, 
	capability VARCHAR(32) NOT NULL, 
	is_enabled BOOLEAN NOT NULL, 
	is_premium BOOLEAN NOT NULL, 
	min_tier VARCHAR(16) NOT NULL, 
	display_order INTEGER NOT NULL, 
	fallback_codes JSONB, 
	region_rules JSONB, 
	max_tokens INTEGER, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	deleted_at TIMESTAMP WITH TIME ZONE, 
	PRIMARY KEY (id), 
	FOREIGN KEY(provider_id) REFERENCES ai_providers (id)
);

CREATE INDEX ix_ai_models_provider_id ON ai_models (provider_id);
CREATE UNIQUE INDEX ix_ai_models_code ON ai_models (code);

CREATE TABLE conversations (
	user_id UUID NOT NULL, 
	title VARCHAR(255) NOT NULL, 
	model_code VARCHAR(64), 
	is_pinned BOOLEAN NOT NULL, 
	is_favorite BOOLEAN NOT NULL, 
	is_archived BOOLEAN NOT NULL, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	deleted_at TIMESTAMP WITH TIME ZONE, 
	PRIMARY KEY (id), 
	FOREIGN KEY(user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_conversations_user_id ON conversations (user_id);

CREATE TABLE devices (
	user_id UUID NOT NULL, 
	device_id VARCHAR(128) NOT NULL, 
	platform VARCHAR(32) NOT NULL, 
	fcm_token VARCHAR(512), 
	app_version VARCHAR(32), 
	last_seen_at TIMESTAMP WITH TIME ZONE, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	CONSTRAINT uq_user_device UNIQUE (user_id, device_id), 
	FOREIGN KEY(user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_devices_user_id ON devices (user_id);

CREATE TABLE payments (
	user_id UUID NOT NULL, 
	provider VARCHAR(32) NOT NULL, 
	product_type VARCHAR(32) NOT NULL, 
	product_id VARCHAR(128), 
	amount NUMERIC(18, 2) NOT NULL, 
	currency VARCHAR(8) NOT NULL, 
	status VARCHAR(16) NOT NULL, 
	provider_ref VARCHAR(255), 
	idempotency_key VARCHAR(128) NOT NULL, 
	verified_at TIMESTAMP WITH TIME ZONE, 
	meta JSONB, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	FOREIGN KEY(user_id) REFERENCES users (id), 
	UNIQUE (idempotency_key)
);

CREATE INDEX ix_payments_user_id ON payments (user_id);
CREATE INDEX ix_payments_provider_ref ON payments (provider_ref);

CREATE TABLE referrals (
	referrer_id UUID NOT NULL, 
	referred_id UUID NOT NULL, 
	reward_credit NUMERIC(18, 2) NOT NULL, 
	status VARCHAR(16) NOT NULL, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	FOREIGN KEY(referrer_id) REFERENCES users (id), 
	UNIQUE (referred_id), 
	FOREIGN KEY(referred_id) REFERENCES users (id)
);

CREATE INDEX ix_referrals_referrer_id ON referrals (referrer_id);

CREATE TABLE sessions (
	user_id UUID NOT NULL, 
	device_id VARCHAR(128), 
	refresh_jti VARCHAR(64) NOT NULL, 
	expires_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	revoked_at TIMESTAMP WITH TIME ZONE, 
	ip_address VARCHAR(64), 
	user_agent VARCHAR(256), 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	FOREIGN KEY(user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_sessions_user_id ON sessions (user_id);
CREATE UNIQUE INDEX ix_sessions_refresh_jti ON sessions (refresh_jti);

CREATE TABLE subscriptions (
	user_id UUID NOT NULL, 
	plan_id UUID NOT NULL, 
	status VARCHAR(16) NOT NULL, 
	started_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	expires_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	auto_renew BOOLEAN NOT NULL, 
	cancelled_at TIMESTAMP WITH TIME ZONE, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	FOREIGN KEY(user_id) REFERENCES users (id) ON DELETE CASCADE, 
	FOREIGN KEY(plan_id) REFERENCES subscription_plans (id)
);

CREATE INDEX ix_subscriptions_user_id ON subscriptions (user_id);

CREATE TABLE user_profiles (
	user_id UUID NOT NULL, 
	display_name VARCHAR(120), 
	avatar_url VARCHAR(512), 
	language VARCHAR(8) NOT NULL, 
	locale VARCHAR(16) NOT NULL, 
	memory_enabled BOOLEAN NOT NULL, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	UNIQUE (user_id), 
	FOREIGN KEY(user_id) REFERENCES users (id) ON DELETE CASCADE
);


CREATE TABLE wallets (
	user_id UUID NOT NULL, 
	balance NUMERIC(18, 2) NOT NULL, 
	locked_balance NUMERIC(18, 2) NOT NULL, 
	lifetime_credit NUMERIC(18, 2) NOT NULL, 
	spent_credit NUMERIC(18, 2) NOT NULL, 
	bonus_credit NUMERIC(18, 2) NOT NULL, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	UNIQUE (user_id), 
	FOREIGN KEY(user_id) REFERENCES users (id) ON DELETE CASCADE
);


CREATE TABLE messages (
	conversation_id UUID NOT NULL, 
	role VARCHAR(16) NOT NULL, 
	content TEXT NOT NULL, 
	model_code VARCHAR(64), 
	input_tokens INTEGER NOT NULL, 
	output_tokens INTEGER NOT NULL, 
	status VARCHAR(16) NOT NULL, 
	meta JSONB, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	deleted_at TIMESTAMP WITH TIME ZONE, 
	PRIMARY KEY (id), 
	FOREIGN KEY(conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
);

CREATE INDEX ix_messages_conversation_id ON messages (conversation_id);

CREATE TABLE model_pricing (
	model_id UUID NOT NULL, 
	provider_cost_input NUMERIC(18, 6) NOT NULL, 
	provider_cost_output NUMERIC(18, 6) NOT NULL, 
	provider_cost_unit NUMERIC(18, 6) NOT NULL, 
	internal_cost NUMERIC(18, 6) NOT NULL, 
	user_price_input NUMERIC(18, 6) NOT NULL, 
	user_price_output NUMERIC(18, 6) NOT NULL, 
	user_price_unit NUMERIC(18, 6) NOT NULL, 
	minimum_charge NUMERIC(18, 2) NOT NULL, 
	minimum_profit NUMERIC(18, 6) NOT NULL, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	UNIQUE (model_id), 
	FOREIGN KEY(model_id) REFERENCES ai_models (id) ON DELETE CASCADE
);


CREATE TABLE usage_logs (
	user_id UUID NOT NULL, 
	model_id UUID, 
	provider_id UUID, 
	request_type VARCHAR(32) NOT NULL, 
	request_id VARCHAR(64) NOT NULL, 
	input_tokens INTEGER NOT NULL, 
	output_tokens INTEGER NOT NULL, 
	provider_cost NUMERIC(18, 6) NOT NULL, 
	charged_credit NUMERIC(18, 2) NOT NULL, 
	gross_profit NUMERIC(18, 6) NOT NULL, 
	latency_ms INTEGER NOT NULL, 
	status VARCHAR(16) NOT NULL, 
	meta JSONB, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	CONSTRAINT uq_usage_request_id UNIQUE (request_id), 
	FOREIGN KEY(user_id) REFERENCES users (id), 
	FOREIGN KEY(model_id) REFERENCES ai_models (id), 
	FOREIGN KEY(provider_id) REFERENCES ai_providers (id)
);

CREATE INDEX ix_usage_logs_request_id ON usage_logs (request_id);
CREATE INDEX ix_usage_logs_user_id ON usage_logs (user_id);

CREATE TABLE wallet_transactions (
	wallet_id UUID NOT NULL, 
	type VARCHAR(32) NOT NULL, 
	amount NUMERIC(18, 2) NOT NULL, 
	balance_after NUMERIC(18, 2) NOT NULL, 
	reason VARCHAR(255), 
	reference_type VARCHAR(64), 
	reference_id VARCHAR(128), 
	idempotency_key VARCHAR(128), 
	meta JSONB, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	CONSTRAINT uq_wallet_tx_idempotency UNIQUE (idempotency_key), 
	FOREIGN KEY(wallet_id) REFERENCES wallets (id) ON DELETE CASCADE
);

CREATE INDEX ix_wallet_transactions_wallet_id ON wallet_transactions (wallet_id);

CREATE TABLE message_attachments (
	message_id UUID NOT NULL, 
	kind VARCHAR(16) NOT NULL, 
	storage_key VARCHAR(512) NOT NULL, 
	mime_type VARCHAR(120), 
	size_bytes INTEGER, 
	id UUID NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL, 
	PRIMARY KEY (id), 
	FOREIGN KEY(message_id) REFERENCES messages (id) ON DELETE CASCADE
);

CREATE INDEX ix_message_attachments_message_id ON message_attachments (message_id);
