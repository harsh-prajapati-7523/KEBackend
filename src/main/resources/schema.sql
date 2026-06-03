-- Employee bootstrap users are managed by BootstrapSuperAdminInitializer.
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT now();

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
