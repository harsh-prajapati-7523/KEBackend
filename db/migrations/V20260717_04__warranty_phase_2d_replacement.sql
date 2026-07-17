-- Warranty Module Phase 2D. Apply after V20260717_03. Test environment only until production approval.
CREATE TABLE IF NOT EXISTS warranty_replacements (
 id BIGSERIAL PRIMARY KEY,
 warranty_claim_id BIGINT NOT NULL REFERENCES warranty_claims(id), replacement_sequence INTEGER NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 replacement_received_date DATE NOT NULL, replacement_given_to_customer_date DATE,
 new_product_name VARCHAR(160), new_product_type VARCHAR(120), new_model_number VARCHAR(120),
 new_serial_number VARCHAR(160), normalized_new_serial_number VARCHAR(160),
 new_warranty_start_date DATE, new_warranty_end_date DATE,
 replacement_reference_number VARCHAR(160), normalized_reference_number VARCHAR(160), replacement_provided_by VARCHAR(160) NOT NULL,
 replacement_notes VARCHAR(2000), serial_number_override_reason VARCHAR(1000), reference_override_reason VARCHAR(1000), supersede_reason VARCHAR(1000),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_by_employee_id VARCHAR(80) NOT NULL, updated_by_employee_id VARCHAR(80) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_warranty_replacement_sequence UNIQUE(warranty_claim_id,replacement_sequence),
 CONSTRAINT ck_warranty_replacement_product CHECK (new_product_name IS NOT NULL OR new_product_type IS NOT NULL),
 CONSTRAINT ck_warranty_replacement_serial CHECK (new_serial_number IS NOT NULL OR serial_number_override_reason IS NOT NULL),
 CONSTRAINT ck_warranty_replacement_reference CHECK (replacement_reference_number IS NOT NULL OR reference_override_reason IS NOT NULL),
 CONSTRAINT ck_warranty_replacement_warranty_dates CHECK (new_warranty_end_date IS NULL OR new_warranty_start_date IS NULL OR new_warranty_end_date >= new_warranty_start_date),
 CONSTRAINT ck_warranty_replacement_delivery_date CHECK (replacement_given_to_customer_date IS NULL OR replacement_given_to_customer_date >= replacement_received_date)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_warranty_replacements_one_active ON warranty_replacements(warranty_claim_id) WHERE active=true;
CREATE INDEX IF NOT EXISTS idx_warranty_replacements_claim ON warranty_replacements(warranty_claim_id);
CREATE INDEX IF NOT EXISTS idx_warranty_replacements_serial ON warranty_replacements(normalized_new_serial_number);
CREATE INDEX IF NOT EXISTS idx_warranty_replacements_reference ON warranty_replacements(normalized_reference_number);
CREATE INDEX IF NOT EXISTS idx_warranty_replacements_received ON warranty_replacements(replacement_received_date);
CREATE INDEX IF NOT EXISTS idx_warranty_replacements_given ON warranty_replacements(replacement_given_to_customer_date);
CREATE INDEX IF NOT EXISTS idx_warranty_replacements_created ON warranty_replacements(created_at);
CREATE INDEX IF NOT EXISTS idx_warranty_replacements_updated ON warranty_replacements(updated_at);

ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_state;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_state CHECK (state IN (
 'DETAILS_REQUIRED','COMPLAINT_REQUIRED','COMPLAINT_REGISTRATION_PENDING','VISIT_SCHEDULING_REQUIRED','VISIT_PENDING',
 'VISIT_COMPLETED_AWAITING_RESULT','AWAITING_RESULT','MANUFACTURER_REPAIR_COMPLETED','REPLACEMENT_APPROVED_AWAITING_PRODUCT','REPLACEMENT_RECEIVED',
 'WARRANTY_REJECTED_AWAITING_DECISION','CUSTOMER_DECISION_PENDING','RESOLVED','DELIVERY_PENDING','CLOSED'
));
ALTER TABLE warranty_claim_events DROP CONSTRAINT IF EXISTS ck_warranty_claim_events_type;
ALTER TABLE warranty_claim_events ADD CONSTRAINT ck_warranty_claim_events_type CHECK (event_type IN (
 'WARRANTY_CLAIM_CREATED','WARRANTY_DETAILS_UPDATED','WARRANTY_OWNER_CHANGED','WARRANTY_STATE_CHANGED','WARRANTY_CLAIM_CLOSED',
 'WARRANTY_COMPLAINT_REGISTRATION_PENDING','WARRANTY_COMPLAINT_REGISTERED','WARRANTY_VISIT_SCHEDULED','WARRANTY_EXPECTED_VISIT_UPDATED',
 'WARRANTY_VISIT_RECORDED','WARRANTY_FOLLOW_UP_SCHEDULED','WARRANTY_FOLLOW_UP_UPDATED','WARRANTY_FOLLOW_UP_CLEARED',
 'WARRANTY_MANUFACTURER_REPAIR_COMPLETED','WARRANTY_REJECTED','WARRANTY_REJECTION_DECISION_RECORDED','WARRANTY_CUSTOMER_DID_NOT_PROCEED',
 'WARRANTY_REPLACEMENT_APPROVED','WARRANTY_MOVED_TO_READY_DELIVERY','WARRANTY_CONTINUED_AS_PAID_REPAIR','WARRANTY_REPLACEMENT_RECEIVED',
 'WARRANTY_REPLACEMENT_DETAILS_UPDATED','WARRANTY_REPLACEMENT_SUPERSEDED','WARRANTY_REPLACEMENT_READY_FOR_DELIVERY',
 'WARRANTY_REPLACEMENT_GIVEN_TO_CUSTOMER','WARRANTY_REPLACEMENT_CLAIM_CLOSED'
));
