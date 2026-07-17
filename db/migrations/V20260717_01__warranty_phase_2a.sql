-- Warranty Module Phase 2A. Apply manually with psql before deploying the matching application.
-- Safe for existing PostgreSQL databases; creates only Phase 2A warranty objects.

CREATE TABLE IF NOT EXISTS warranty_claims (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    claim_sequence INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    state VARCHAR(40) NOT NULL,
    result VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    billing_date DATE,
    warranty_start_date DATE,
    warranty_end_date DATE,
    manufacturer_name VARCHAR(120),
    product_serial_number VARCHAR(120),
    model_number VARCHAR(120),
    manufacturer_complaint_number VARCHAR(120),
    complaint_registered_date DATE,
    expected_visit_date DATE,
    manufacturer_engineer_name VARCHAR(120),
    manufacturer_engineer_mobile VARCHAR(20),
    manufacturer_service_center_name VARCHAR(160),
    warranty_notes VARCHAR(2000),
    next_follow_up_date DATE,
    warranty_owner_employee_id VARCHAR(80),
    marked_warranty_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by_employee_id VARCHAR(80) NOT NULL,
    updated_by_employee_id VARCHAR(80) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_warranty_claims_ticket_sequence UNIQUE (ticket_id, claim_sequence),
    CONSTRAINT ck_warranty_claims_sequence CHECK (claim_sequence > 0),
    CONSTRAINT ck_warranty_claims_state CHECK (state IN ('DETAILS_REQUIRED','COMPLAINT_REQUIRED','VISIT_PENDING','AWAITING_RESULT','RESOLVED','DELIVERY_PENDING','CLOSED')),
    CONSTRAINT ck_warranty_claims_result CHECK (result IN ('PENDING','MANUFACTURER_REPAIR_DONE','REPLACEMENT_DONE','WARRANTY_REJECTED','CUSTOMER_DID_NOT_PROCEED')),
    CONSTRAINT ck_warranty_claims_warranty_dates CHECK (warranty_end_date IS NULL OR warranty_start_date IS NULL OR warranty_end_date >= warranty_start_date)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_warranty_claims_one_active_ticket
    ON warranty_claims(ticket_id) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_warranty_claims_ticket ON warranty_claims(ticket_id);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_state ON warranty_claims(state);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_result ON warranty_claims(result);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_owner ON warranty_claims(warranty_owner_employee_id);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_manufacturer ON warranty_claims(lower(manufacturer_name));
CREATE INDEX IF NOT EXISTS idx_warranty_claims_complaint ON warranty_claims(manufacturer_complaint_number);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_follow_up ON warranty_claims(next_follow_up_date);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_expected_visit ON warranty_claims(expected_visit_date);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_created ON warranty_claims(created_at);

CREATE TABLE IF NOT EXISTS warranty_claim_events (
    id BIGSERIAL PRIMARY KEY,
    warranty_claim_id BIGINT NOT NULL REFERENCES warranty_claims(id),
    ticket_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_employee_id VARCHAR(80) NOT NULL,
    actor_name_snapshot VARCHAR(120),
    event_timestamp TIMESTAMP NOT NULL,
    changed_fields_json TEXT,
    summary VARCHAR(500) NOT NULL,
    CONSTRAINT ck_warranty_claim_events_type CHECK (event_type IN ('WARRANTY_CLAIM_CREATED','WARRANTY_DETAILS_UPDATED','WARRANTY_OWNER_CHANGED','WARRANTY_STATE_CHANGED','WARRANTY_CLAIM_CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_warranty_claim_events_claim_time
    ON warranty_claim_events(warranty_claim_id, event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_warranty_claim_events_ticket
    ON warranty_claim_events(ticket_id);
