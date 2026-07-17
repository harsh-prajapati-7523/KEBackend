-- Warranty Phase 6 internal reminders. Apply after V20260717_07. Test environment only.
CREATE TABLE IF NOT EXISTS warranty_reminder_generation_runs(
 id BIGSERIAL PRIMARY KEY,started_at TIMESTAMPTZ NOT NULL,completed_at TIMESTAMPTZ,business_date DATE NOT NULL,status VARCHAR(20) NOT NULL,
 claims_scanned INTEGER NOT NULL DEFAULT 0,reminders_created INTEGER NOT NULL DEFAULT 0,reminders_updated INTEGER NOT NULL DEFAULT 0,
 reminders_resolved INTEGER NOT NULL DEFAULT 0,failures INTEGER NOT NULL DEFAULT 0,error_summary VARCHAR(1000),
 CONSTRAINT ck_warranty_reminder_run_status CHECK(status IN('RUNNING','COMPLETED','COMPLETED_WITH_FAILURES','FAILED')));
CREATE TABLE IF NOT EXISTS warranty_reminders(
 id BIGSERIAL PRIMARY KEY,warranty_claim_id BIGINT NOT NULL REFERENCES warranty_claims(id),ticket_id BIGINT NOT NULL REFERENCES tickets(id),
 reminder_type VARCHAR(50) NOT NULL,status VARCHAR(20) NOT NULL,priority VARCHAR(20) NOT NULL,recipient_employee_id VARCHAR(80),recipient_employee_name_snapshot VARCHAR(160),
 due_date DATE,generated_for_date DATE NOT NULL,title VARCHAR(180) NOT NULL,message VARCHAR(1000) NOT NULL,pending_action_code VARCHAR(80),
 overdue BOOLEAN NOT NULL DEFAULT FALSE,days_overdue INTEGER NOT NULL DEFAULT 0,escalated BOOLEAN NOT NULL DEFAULT FALSE,escalation_level VARCHAR(20) NOT NULL DEFAULT 'NONE',
 deduplication_key VARCHAR(300) NOT NULL,acknowledged_at TIMESTAMPTZ,acknowledged_by_employee_id VARCHAR(80),acknowledgement_note VARCHAR(1000),
 snoozed_until TIMESTAMPTZ,snoozed_by_employee_id VARCHAR(80),snooze_reason VARCHAR(1000),resolved_at TIMESTAMPTZ,resolved_reason VARCHAR(500),created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_warranty_reminders_dedup UNIQUE(deduplication_key),CONSTRAINT ck_warranty_reminder_status CHECK(status IN('OPEN','ACKNOWLEDGED','SNOOZED','RESOLVED','SUPERSEDED')),
 CONSTRAINT ck_warranty_reminder_priority CHECK(priority IN('LOW','NORMAL','HIGH','CRITICAL')),CONSTRAINT ck_warranty_reminder_escalation CHECK(escalation_level IN('NONE','LEVEL_1','LEVEL_2','LEVEL_3')),
 CONSTRAINT ck_warranty_reminder_days CHECK(days_overdue>=0),CONSTRAINT ck_warranty_reminder_type CHECK(reminder_type IN('FOLLOW_UP_DUE','FOLLOW_UP_OVERDUE','EXPECTED_VISIT_DUE','EXPECTED_VISIT_OVERDUE','COMPLAINT_REGISTRATION_PENDING','COMPLAINT_FOLLOW_UP_DUE','WARRANTY_RESULT_PENDING','EXPECTED_REPLACEMENT_DUE','EXPECTED_REPLACEMENT_OVERDUE','REPLACEMENT_READY_FOR_DELIVERY','READY_TO_DELIVER','AWAITING_CUSTOMER_DECISION','REQUIRED_DOCUMENTS_MISSING','WARRANTY_OWNER_MISSING','LONG_PENDING_WARRANTY_CASE')));
CREATE INDEX IF NOT EXISTS idx_warranty_reminders_recipient_status ON warranty_reminders(recipient_employee_id,status);
CREATE INDEX IF NOT EXISTS idx_warranty_reminders_due ON warranty_reminders(due_date,status);
CREATE INDEX IF NOT EXISTS idx_warranty_reminders_escalation ON warranty_reminders(escalated,priority,status);
CREATE INDEX IF NOT EXISTS idx_warranty_reminders_claim ON warranty_reminders(warranty_claim_id);
CREATE INDEX IF NOT EXISTS idx_warranty_reminders_ticket ON warranty_reminders(ticket_id);
CREATE INDEX IF NOT EXISTS idx_warranty_reminder_runs_date ON warranty_reminder_generation_runs(business_date,started_at DESC);
