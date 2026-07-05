\set ON_ERROR_STOP on

CREATE TABLE IF NOT EXISTS ticket_suggestion_terms (
    id BIGSERIAL PRIMARY KEY,
    suggestion_type VARCHAR(30) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    display_value VARCHAR(255) NOT NULL,
    usage_count BIGINT NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ticket_suggestion_terms_type_normalized UNIQUE (suggestion_type, normalized_value)
);

CREATE OR REPLACE FUNCTION normalize_ticket_suggestion_display(input_text text)
RETURNS text AS $$
DECLARE
    normalized text;
BEGIN
    normalized := INITCAP(LOWER(REGEXP_REPLACE(BTRIM(input_text), '\s+', ' ', 'g')));

    normalized := REGEXP_REPLACE(normalized, '\mUps\M', 'UPS', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mLed\M', 'LED', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mTv\M', 'TV', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mAc\M', 'AC', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mDc\M', 'DC', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mMcb\M', 'MCB', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mPcb\M', 'PCB', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mUsb\M', 'USB', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mCctv\M', 'CCTV', 'g');
    normalized := REGEXP_REPLACE(normalized, '\mRo\M', 'RO', 'g');

    RETURN normalized;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

WITH product_terms AS (
    SELECT
        'PRODUCT_TYPE' AS suggestion_type,
        LOWER(REGEXP_REPLACE(BTRIM(product_type), '\s+', ' ', 'g')) AS normalized_value,
        normalize_ticket_suggestion_display(product_type) AS display_value,
        COUNT(*) AS usage_count,
        MAX(created_at) AS last_used_at
    FROM tickets
    WHERE product_type IS NOT NULL
      AND BTRIM(product_type) <> ''
    GROUP BY LOWER(REGEXP_REPLACE(BTRIM(product_type), '\s+', ' ', 'g')), normalize_ticket_suggestion_display(product_type)
)
INSERT INTO ticket_suggestion_terms (
    suggestion_type,
    normalized_value,
    display_value,
    usage_count,
    last_used_at,
    created_at,
    updated_at
)
SELECT
    suggestion_type,
    normalized_value,
    display_value,
    usage_count,
    last_used_at,
    now(),
    now()
FROM product_terms
WHERE length(normalized_value) <= 255
  AND length(display_value) <= 255
ON CONFLICT (suggestion_type, normalized_value)
DO UPDATE SET
    display_value = EXCLUDED.display_value,
    usage_count = EXCLUDED.usage_count,
    last_used_at = EXCLUDED.last_used_at,
    updated_at = now();

WITH village_terms AS (
    SELECT
        'VILLAGE_OR_AREA' AS suggestion_type,
        LOWER(REGEXP_REPLACE(BTRIM(village_or_area), '\s+', ' ', 'g')) AS normalized_value,
        normalize_ticket_suggestion_display(village_or_area) AS display_value,
        COUNT(*) AS usage_count,
        MAX(created_at) AS last_used_at
    FROM tickets
    WHERE village_or_area IS NOT NULL
      AND BTRIM(village_or_area) <> ''
    GROUP BY LOWER(REGEXP_REPLACE(BTRIM(village_or_area), '\s+', ' ', 'g')), normalize_ticket_suggestion_display(village_or_area)
)
INSERT INTO ticket_suggestion_terms (
    suggestion_type,
    normalized_value,
    display_value,
    usage_count,
    last_used_at,
    created_at,
    updated_at
)
SELECT
    suggestion_type,
    normalized_value,
    display_value,
    usage_count,
    last_used_at,
    now(),
    now()
FROM village_terms
WHERE length(normalized_value) <= 255
  AND length(display_value) <= 255
ON CONFLICT (suggestion_type, normalized_value)
DO UPDATE SET
    display_value = EXCLUDED.display_value,
    usage_count = EXCLUDED.usage_count,
    last_used_at = EXCLUDED.last_used_at,
    updated_at = now();

CREATE INDEX IF NOT EXISTS idx_ticket_suggestion_terms_type_prefix
    ON ticket_suggestion_terms (suggestion_type, normalized_value text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_ticket_suggestion_terms_type_count
    ON ticket_suggestion_terms (suggestion_type, usage_count DESC, normalized_value);

ANALYZE ticket_suggestion_terms;

SELECT suggestion_type, COUNT(*) AS suggestion_count
FROM ticket_suggestion_terms
GROUP BY suggestion_type
ORDER BY suggestion_type;
