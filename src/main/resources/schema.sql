-- Employee bootstrap users are managed by BootstrapSuperAdminInitializer.
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_key VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE roles
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE roles
    ALTER COLUMN system_role SET DEFAULT FALSE;

ALTER TABLE roles
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE roles
    ALTER COLUMN updated_at SET DEFAULT now();

UPDATE roles
SET created_at = now()
WHERE created_at IS NULL;

UPDATE roles
SET updated_at = now()
WHERE updated_at IS NULL;

INSERT INTO roles (role_key, display_name, active, system_role, created_at, updated_at)
VALUES
    ('SUPER_ADMIN', 'Super Admin', TRUE, TRUE, now(), now()),
    ('ADMIN', 'Admin', TRUE, TRUE, now(), now()),
    ('EMPLOYEE', 'Employee', TRUE, TRUE, now(), now()),
    ('TECHNICIAN', 'Technician', TRUE, TRUE, now(), now())
ON CONFLICT (role_key) DO UPDATE
SET
    display_name = EXCLUDED.display_name,
    active = CASE
        WHEN roles.role_key = 'SUPER_ADMIN' THEN TRUE
        ELSE roles.active
    END,
    system_role = TRUE,
    updated_at = now();

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS role_id BIGINT;

ALTER TABLE employees
    ALTER COLUMN role DROP NOT NULL;

UPDATE employees e
SET role_id = r.id
FROM roles r
WHERE e.role = r.role_key
  AND e.role_id IS NULL;

ALTER TABLE employees
    DROP CONSTRAINT IF EXISTS fk_employees_role;

ALTER TABLE employees
    ADD CONSTRAINT fk_employees_role FOREIGN KEY (role_id)
    REFERENCES roles(id);

ALTER TABLE employees
    DROP CONSTRAINT IF EXISTS ck_employees_role_backfilled;

ALTER TABLE employees
    ADD CONSTRAINT ck_employees_role_backfilled
    CHECK (role IS NULL OR role_id IS NOT NULL)
    NOT VALID;

ALTER TABLE employees
    VALIDATE CONSTRAINT ck_employees_role_backfilled;

--Ticket Number Sequence Creation
CREATE SEQUENCE IF NOT EXISTS ticket_number_seq
    START WITH 1
    INCREMENT BY 1;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS picked_by_employee_id VARCHAR;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS completed_by_employee_id VARCHAR;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS completion_remark VARCHAR(1000);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS cancelled_by_employee_id VARCHAR;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(1000);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS warranty_status VARCHAR NOT NULL DEFAULT 'NOT_CHECKED';

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS manufacturer_status VARCHAR NOT NULL DEFAULT 'NOT_REQUIRED';

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS manufacturer_complaint_number VARCHAR(80);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS manufacturer_or_brand_name VARCHAR(80);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS product_serial_number VARCHAR(80);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS warranty_updated_at TIMESTAMP;

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS warranty_updated_by_employee_id VARCHAR;

CREATE TABLE IF NOT EXISTS ticket_charge_items (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    description VARCHAR(120) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by_employee_id VARCHAR NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by_employee_id VARCHAR,
    CONSTRAINT fk_ticket_charge_items_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ticket_charge_items_ticket_id ON ticket_charge_items(ticket_id);

CREATE INDEX IF NOT EXISTS idx_tickets_mobile_number ON tickets(mobile_number);
