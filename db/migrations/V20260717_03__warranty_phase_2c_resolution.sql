-- Warranty Module Phase 2C. Apply after V20260717_02. Test environment only until production approval.
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS manufacturer_repair_completed_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS manufacturer_repair_completion_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS manufacturer_repair_completion_notes VARCHAR(1500);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS manufacturer_repair_reference VARCHAR(120);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS warranty_rejected_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS warranty_rejection_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS warranty_rejection_reason VARCHAR(1500);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS rejection_decision VARCHAR(40);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS customer_decision_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS customer_decision_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS customer_decision_notes VARCHAR(1500);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS customer_did_not_proceed_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS customer_did_not_proceed_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS customer_did_not_proceed_reason VARCHAR(1500);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS customer_journey_decision VARCHAR(40);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS replacement_approved_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS replacement_approval_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS replacement_approval_reference VARCHAR(120);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS expected_replacement_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS replacement_approval_notes VARCHAR(1500);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS claim_closed_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS claim_closed_by_employee_id VARCHAR(80);

ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_state;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_state CHECK (state IN (
 'DETAILS_REQUIRED','COMPLAINT_REQUIRED','COMPLAINT_REGISTRATION_PENDING','VISIT_SCHEDULING_REQUIRED','VISIT_PENDING',
 'VISIT_COMPLETED_AWAITING_RESULT','AWAITING_RESULT','MANUFACTURER_REPAIR_COMPLETED','REPLACEMENT_APPROVED_AWAITING_PRODUCT',
 'WARRANTY_REJECTED_AWAITING_DECISION','CUSTOMER_DECISION_PENDING','RESOLVED','DELIVERY_PENDING','CLOSED'
));
ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_rejection_decision;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_rejection_decision CHECK (rejection_decision IS NULL OR rejection_decision IN ('CONTINUE_AS_PAID_REPAIR','AWAIT_CUSTOMER_DECISION','CLOSE_WARRANTY_CLAIM'));
ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_customer_journey_decision;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_customer_journey_decision CHECK (customer_journey_decision IS NULL OR customer_journey_decision IN ('CONTINUE_AS_PAID_REPAIR','CLOSE_WARRANTY_CLAIM','AWAIT_CUSTOMER_COLLECTION'));

ALTER TABLE warranty_claim_events DROP CONSTRAINT IF EXISTS ck_warranty_claim_events_type;
ALTER TABLE warranty_claim_events ADD CONSTRAINT ck_warranty_claim_events_type CHECK (event_type IN (
 'WARRANTY_CLAIM_CREATED','WARRANTY_DETAILS_UPDATED','WARRANTY_OWNER_CHANGED','WARRANTY_STATE_CHANGED','WARRANTY_CLAIM_CLOSED',
 'WARRANTY_COMPLAINT_REGISTRATION_PENDING','WARRANTY_COMPLAINT_REGISTERED','WARRANTY_VISIT_SCHEDULED','WARRANTY_EXPECTED_VISIT_UPDATED',
 'WARRANTY_VISIT_RECORDED','WARRANTY_FOLLOW_UP_SCHEDULED','WARRANTY_FOLLOW_UP_UPDATED','WARRANTY_FOLLOW_UP_CLEARED',
 'WARRANTY_MANUFACTURER_REPAIR_COMPLETED','WARRANTY_REJECTED','WARRANTY_REJECTION_DECISION_RECORDED',
 'WARRANTY_CUSTOMER_DID_NOT_PROCEED','WARRANTY_REPLACEMENT_APPROVED','WARRANTY_MOVED_TO_READY_DELIVERY','WARRANTY_CONTINUED_AS_PAID_REPAIR'
));
CREATE INDEX IF NOT EXISTS idx_warranty_claims_repair_completed ON warranty_claims(manufacturer_repair_completed_at);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_rejected ON warranty_claims(warranty_rejected_at);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_replacement_approved ON warranty_claims(replacement_approved_at);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_closed ON warranty_claims(claim_closed_at);
