\set ON_ERROR_STOP on

CREATE TABLE IF NOT EXISTS speech_usage_monthly (
    id BIGSERIAL PRIMARY KEY,
    year_month VARCHAR(7) NOT NULL,
    provider_key VARCHAR(40) NOT NULL,
    used_seconds BIGINT NOT NULL DEFAULT 0,
    monthly_limit_seconds BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_speech_usage_monthly_provider_month UNIQUE (provider_key, year_month)
);

CREATE INDEX IF NOT EXISTS idx_speech_usage_monthly_month_provider
    ON speech_usage_monthly (year_month, provider_key);
