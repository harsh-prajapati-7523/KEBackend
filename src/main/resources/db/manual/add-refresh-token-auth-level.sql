ALTER TABLE employee_refresh_tokens
    ADD COLUMN IF NOT EXISTS auth_level VARCHAR(20) NOT NULL DEFAULT 'FULL';

ALTER TABLE employee_refresh_tokens
    DROP CONSTRAINT IF EXISTS ck_employee_refresh_tokens_auth_level;

ALTER TABLE employee_refresh_tokens
    ADD CONSTRAINT ck_employee_refresh_tokens_auth_level
    CHECK (auth_level IN ('PRE_PIN', 'FULL'));
