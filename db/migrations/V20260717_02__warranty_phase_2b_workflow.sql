-- Warranty Module Phase 2B. Apply after V20260717_01 and before deploying Phase 2B.
-- Non-destructive and safe to re-run on PostgreSQL.

ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS complaint_registration_attempt_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS complaint_contact_method VARCHAR(40);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS complaint_attempt_notes VARCHAR(1000);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS actual_visit_date DATE;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS visit_outcome VARCHAR(50);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS visit_notes VARCHAR(1500);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS follow_up_notes VARCHAR(1000);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS follow_up_source VARCHAR(40);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS last_follow_up_scheduled_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS last_follow_up_updated_by_employee_id VARCHAR(80);
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS complaint_registered_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS visit_scheduled_at TIMESTAMP;
ALTER TABLE warranty_claims ADD COLUMN IF NOT EXISTS visit_recorded_at TIMESTAMP;

ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_state;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_state CHECK (state IN (
  'DETAILS_REQUIRED','COMPLAINT_REQUIRED','COMPLAINT_REGISTRATION_PENDING',
  'VISIT_SCHEDULING_REQUIRED','VISIT_PENDING','VISIT_COMPLETED_AWAITING_RESULT',
  'AWAITING_RESULT','RESOLVED','DELIVERY_PENDING','CLOSED'
));

ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_contact_method;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_contact_method CHECK (
  complaint_contact_method IS NULL OR complaint_contact_method IN ('PHONE','WHATSAPP','EMAIL','SERVICE_CENTER_VISIT','MANUFACTURER_PORTAL','OTHER')
);
ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_visit_outcome;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_visit_outcome CHECK (
  visit_outcome IS NULL OR visit_outcome IN ('INSPECTION_COMPLETED','REPAIR_ATTEMPTED','PART_REQUIRED','PRODUCT_TAKEN_TO_SERVICE_CENTER','FOLLOW_UP_REQUIRED','NO_ACTION_TAKEN','ENGINEER_DID_NOT_VISIT','OTHER')
);
ALTER TABLE warranty_claims DROP CONSTRAINT IF EXISTS ck_warranty_claims_follow_up_source;
ALTER TABLE warranty_claims ADD CONSTRAINT ck_warranty_claims_follow_up_source CHECK (
  follow_up_source IS NULL OR follow_up_source IN ('MANUAL','EXPECTED_VISIT_DEFAULT','COMPLAINT_REGISTRATION_DEFAULT')
);

ALTER TABLE warranty_claim_events DROP CONSTRAINT IF EXISTS ck_warranty_claim_events_type;
ALTER TABLE warranty_claim_events ADD CONSTRAINT ck_warranty_claim_events_type CHECK (event_type IN (
  'WARRANTY_CLAIM_CREATED','WARRANTY_DETAILS_UPDATED','WARRANTY_OWNER_CHANGED','WARRANTY_STATE_CHANGED','WARRANTY_CLAIM_CLOSED',
  'WARRANTY_COMPLAINT_REGISTRATION_PENDING','WARRANTY_COMPLAINT_REGISTERED','WARRANTY_VISIT_SCHEDULED',
  'WARRANTY_EXPECTED_VISIT_UPDATED','WARRANTY_VISIT_RECORDED','WARRANTY_FOLLOW_UP_SCHEDULED',
  'WARRANTY_FOLLOW_UP_UPDATED','WARRANTY_FOLLOW_UP_CLEARED'
));

CREATE INDEX IF NOT EXISTS idx_warranty_claims_active_follow_up ON warranty_claims(next_follow_up_date) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_warranty_claims_active_expected_visit ON warranty_claims(expected_visit_date) WHERE active = TRUE;
