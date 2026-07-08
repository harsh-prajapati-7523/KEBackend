\set ON_ERROR_STOP on

-- Safe backend performance indexes for existing read paths.
-- Run in psql. CREATE INDEX CONCURRENTLY cannot run inside an explicit transaction.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_created_at_desc
    ON tickets (created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_owner_created_at_desc
    ON tickets (picked_by_employee_id, created_at DESC)
    WHERE picked_by_employee_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_status_created_at_desc
    ON tickets (status, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_legacy_category_created_at_desc
    ON tickets (category, created_at DESC)
    WHERE category IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_lower_ticket_number
    ON tickets (lower(ticket_number));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_mobile_number_created_at_desc
    ON tickets (mobile_number, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_workflow_history_ticket_created_id_desc
    ON ticket_workflow_history (ticket_id, created_at DESC, id DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_charge_items_active_ticket_created_at
    ON ticket_charge_items (ticket_id, created_at ASC)
    WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_dynamic_values_ticket_created_id
    ON ticket_dynamic_values (ticket_id, created_at ASC, id ASC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_employees_lower_employee_id
    ON employees (lower(employee_id));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_suggestion_terms_type_prefix
    ON ticket_suggestion_terms (suggestion_type, normalized_value text_pattern_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_suggestion_terms_type_count_desc
    ON ticket_suggestion_terms (suggestion_type, usage_count DESC, normalized_value);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_workflow_transitions_from_status_active_sort_id
    ON workflow_transitions (from_status_id, active, sort_order, id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_workflow_transitions_action_from_status_active_id
    ON workflow_transitions (action_key, from_status_id, active, id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_workflow_transition_category_rules_transition_category_active
    ON workflow_transition_category_rules (workflow_transition_id, category_id, active);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_workflow_transition_role_rules_transition_role_active
    ON workflow_transition_role_rules (workflow_transition_id, role_id, active);
