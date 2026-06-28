/*
 * Repair workflow production metadata migration draft.
 *
 * DRAFT ONLY - DO NOT RUN WITHOUT PRODUCT OWNER AND DBA APPROVAL.
 *
 * Purpose:
 * - Seed/update repair workflow metadata using stable keys only.
 * - Keep production categories LEGACY_FIXED until a separate activation approval.
 *
 * This file intentionally does NOT:
 * - switch any category to DB_CONFIGURED
 * - disable fixed workflow
 * - grant ADMIN or TECHNICIAN rollout
 * - copy hosted-test IDs
 * - delete workflow history
 *
 * Stable keys used:
 * - status_key
 * - action_key / access_key
 * - from_status_key
 * - to_status_key
 * - category_key
 * - role_key
 *
 * Manual input required before enabling the category-scope section:
 *   \set selected_category_key 'APPROVED_PRODUCTION_REPAIR_CATEGORY_KEY'
 */

BEGIN;

-- ============================================================
-- SECTION 1: Metadata only.
-- Safe to review before activation. Does not scope a production
-- category and does not change ticket_categories workflow mode.
-- ============================================================

WITH required_statuses(status_key, display_name, behavior_bucket, terminal, sort_order) AS (
    VALUES
        ('MISSING_PART', 'Missing Part', 'IN_PROGRESS', false, 110),
        ('PART_AVAILABLE', 'Part Available', 'IN_PROGRESS', false, 120),
        ('CUSTOMER_APPROVAL_PENDING', 'Customer Approval Pending', 'IN_PROGRESS', false, 130),
        ('IN_WARRANTY', 'In Warranty', 'IN_PROGRESS', false, 140),
        ('WARRANTY_COMPLAINT_LOGGED', 'Warranty Complaint Logged', 'IN_PROGRESS', false, 150),
        ('REPAIR_COMPLETED', 'Repair Completed / Ready for Delivery', 'IN_PROGRESS', false, 160),
        ('CUSTOMER_DECLINED', 'Customer Declined Repair', 'IN_PROGRESS', false, 170),
        ('CANCELLED_PENDING_DELIVERY', 'Cancelled / Return Pending', 'IN_PROGRESS', false, 180),
        ('DELIVERED_TO_CUSTOMER', 'Delivered To Customer', 'COMPLETED', true, 190)
)
INSERT INTO workflow_statuses (
    status_key,
    display_name,
    behavior_bucket,
    terminal,
    system_status,
    protected_status,
    active,
    sort_order,
    created_at,
    updated_at
)
SELECT
    status_key,
    display_name,
    behavior_bucket,
    terminal,
    false,
    false,
    true,
    sort_order,
    now(),
    now()
FROM required_statuses
ON CONFLICT (status_key) DO UPDATE
SET display_name = EXCLUDED.display_name,
    behavior_bucket = EXCLUDED.behavior_bucket,
    terminal = EXCLUDED.terminal,
    system_status = false,
    protected_status = false,
    active = true,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

WITH required_actions(action_key, display_name, button_label, description, sort_order) AS (
    VALUES
        ('START_REPAIR_WORK', 'Start Work', 'Start Work', 'Move a new repair ticket into work in progress.', 110),
        ('MARK_MISSING_PART', 'Mark Missing Part', 'Mark Missing Part', 'Mark that a required part is missing.', 120),
        ('MARK_PART_AVAILABLE', 'Mark Part Available', 'Mark Part Available', 'Mark that the required part is now available.', 130),
        ('RESUME_WORK', 'Resume Work', 'Resume Work', 'Move the ticket back into active repair work.', 140),
        ('NEED_CUSTOMER_APPROVAL', 'Need Customer Approval', 'Need Customer Approval', 'Move the ticket to customer approval pending.', 150),
        ('CUSTOMER_APPROVED', 'Customer Approved', 'Customer Approved', 'Resume repair after customer approval.', 160),
        ('MARK_IN_WARRANTY', 'Mark In Warranty', 'Mark In Warranty', 'Mark the repair as in warranty workflow.', 170),
        ('LOG_WARRANTY_COMPLAINT', 'Log Warranty Complaint', 'Log Warranty Complaint', 'Record that a warranty complaint has been logged.', 180),
        ('MARK_REPAIR_COMPLETED', 'Mark Repair Completed', 'Mark Repair Completed', 'Mark repair as completed and ready for delivery.', 190),
        ('CUSTOMER_DECLINED_REPAIR', 'Customer Declined Repair', 'Customer Declined Repair', 'Mark that the customer declined the repair.', 200),
        ('CANCEL_PENDING_DELIVERY', 'Cancel / Return Pending', 'Cancel / Return Pending', 'Mark the ticket as cancelled with return pending.', 210),
        ('DELIVER_TO_CUSTOMER', 'Delivered To Customer', 'Delivered To Customer', 'Close the ticket after delivery to customer.', 220)
)
INSERT INTO workflow_actions (
    action_key,
    display_name,
    button_label,
    description,
    active,
    system_action,
    protected_action,
    requires_comment,
    confirmation_required,
    sort_order,
    created_at,
    updated_at
)
SELECT
    action_key,
    display_name,
    button_label,
    description,
    true,
    false,
    false,
    false,
    false,
    sort_order,
    now(),
    now()
FROM required_actions
ON CONFLICT (action_key) DO UPDATE
SET display_name = EXCLUDED.display_name,
    button_label = EXCLUDED.button_label,
    description = EXCLUDED.description,
    active = true,
    system_action = false,
    protected_action = false,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

WITH repair_actions(action_key) AS (
    VALUES
        ('START_REPAIR_WORK'),
        ('MARK_MISSING_PART'),
        ('MARK_PART_AVAILABLE'),
        ('RESUME_WORK'),
        ('NEED_CUSTOMER_APPROVAL'),
        ('CUSTOMER_APPROVED'),
        ('MARK_IN_WARRANTY'),
        ('LOG_WARRANTY_COMPLAINT'),
        ('MARK_REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR'),
        ('CANCEL_PENDING_DELIVERY'),
        ('DELIVER_TO_CUSTOMER')
)
INSERT INTO access_key_metadata (
    access_key,
    category,
    system_key,
    protected_key,
    active,
    created_at,
    updated_at
)
SELECT
    action_key,
    'Repair Workflow',
    false,
    false,
    true,
    now(),
    now()
FROM repair_actions
ON CONFLICT (access_key) DO UPDATE
SET category = EXCLUDED.category,
    system_key = false,
    protected_key = false,
    active = true,
    updated_at = now();

WITH repair_actions(action_key) AS (
    VALUES
        ('START_REPAIR_WORK'),
        ('MARK_MISSING_PART'),
        ('MARK_PART_AVAILABLE'),
        ('RESUME_WORK'),
        ('NEED_CUSTOMER_APPROVAL'),
        ('CUSTOMER_APPROVED'),
        ('MARK_IN_WARRANTY'),
        ('LOG_WARRANTY_COMPLAINT'),
        ('MARK_REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR'),
        ('CANCEL_PENDING_DELIVERY'),
        ('DELIVER_TO_CUSTOMER')
),
super_admin AS (
    SELECT id AS role_id
    FROM roles
    WHERE role_key = 'SUPER_ADMIN'
)
INSERT INTO role_access_rules (
    role_id,
    access_key,
    allowed,
    created_at,
    updated_at
)
SELECT
    super_admin.role_id,
    repair_actions.action_key,
    true,
    now(),
    now()
FROM repair_actions
CROSS JOIN super_admin
ON CONFLICT (role_id, access_key) DO UPDATE
SET allowed = true,
    updated_at = now();

WITH required_transitions(action_key, display_name, from_status_key, to_status_key, sort_order) AS (
    VALUES
        ('START_REPAIR_WORK', 'Start Work', 'NEW', 'IN_PROGRESS', 110),
        ('MARK_MISSING_PART', 'Mark Missing Part', 'IN_PROGRESS', 'MISSING_PART', 120),
        ('NEED_CUSTOMER_APPROVAL', 'Need Customer Approval', 'IN_PROGRESS', 'CUSTOMER_APPROVAL_PENDING', 130),
        ('MARK_IN_WARRANTY', 'Mark In Warranty', 'IN_PROGRESS', 'IN_WARRANTY', 140),
        ('MARK_REPAIR_COMPLETED', 'Mark Repair Completed', 'IN_PROGRESS', 'REPAIR_COMPLETED', 150),
        ('CUSTOMER_DECLINED_REPAIR', 'Customer Declined Repair', 'IN_PROGRESS', 'CUSTOMER_DECLINED', 160),
        ('CANCEL_PENDING_DELIVERY', 'Cancel / Return Pending', 'IN_PROGRESS', 'CANCELLED_PENDING_DELIVERY', 170),
        ('MARK_PART_AVAILABLE', 'Mark Part Available', 'MISSING_PART', 'PART_AVAILABLE', 180),
        ('CUSTOMER_DECLINED_REPAIR', 'Customer Declined Repair', 'MISSING_PART', 'CUSTOMER_DECLINED', 190),
        ('CANCEL_PENDING_DELIVERY', 'Cancel / Return Pending', 'MISSING_PART', 'CANCELLED_PENDING_DELIVERY', 200),
        ('RESUME_WORK', 'Resume Work', 'PART_AVAILABLE', 'IN_PROGRESS', 210),
        ('MARK_REPAIR_COMPLETED', 'Mark Repair Completed', 'PART_AVAILABLE', 'REPAIR_COMPLETED', 220),
        ('MARK_MISSING_PART', 'Mark Missing Part', 'PART_AVAILABLE', 'MISSING_PART', 230),
        ('CUSTOMER_DECLINED_REPAIR', 'Customer Declined Repair', 'PART_AVAILABLE', 'CUSTOMER_DECLINED', 240),
        ('CUSTOMER_APPROVED', 'Customer Approved', 'CUSTOMER_APPROVAL_PENDING', 'IN_PROGRESS', 250),
        ('CUSTOMER_DECLINED_REPAIR', 'Customer Declined Repair', 'CUSTOMER_APPROVAL_PENDING', 'CUSTOMER_DECLINED', 260),
        ('LOG_WARRANTY_COMPLAINT', 'Log Warranty Complaint', 'IN_WARRANTY', 'WARRANTY_COMPLAINT_LOGGED', 270),
        ('CUSTOMER_DECLINED_REPAIR', 'Customer Declined Repair', 'IN_WARRANTY', 'CUSTOMER_DECLINED', 280),
        ('CANCEL_PENDING_DELIVERY', 'Cancel / Return Pending', 'IN_WARRANTY', 'CANCELLED_PENDING_DELIVERY', 290),
        ('MARK_REPAIR_COMPLETED', 'Mark Repair Completed', 'WARRANTY_COMPLAINT_LOGGED', 'REPAIR_COMPLETED', 300),
        ('CUSTOMER_DECLINED_REPAIR', 'Customer Declined Repair', 'WARRANTY_COMPLAINT_LOGGED', 'CUSTOMER_DECLINED', 310),
        ('CANCEL_PENDING_DELIVERY', 'Cancel / Return Pending', 'WARRANTY_COMPLAINT_LOGGED', 'CANCELLED_PENDING_DELIVERY', 320),
        ('DELIVER_TO_CUSTOMER', 'Delivered To Customer', 'REPAIR_COMPLETED', 'DELIVERED_TO_CUSTOMER', 330),
        ('DELIVER_TO_CUSTOMER', 'Delivered To Customer', 'CUSTOMER_DECLINED', 'DELIVERED_TO_CUSTOMER', 340),
        ('DELIVER_TO_CUSTOMER', 'Delivered To Customer', 'CANCELLED_PENDING_DELIVERY', 'DELIVERED_TO_CUSTOMER', 350)
),
resolved AS (
    SELECT
        required_transitions.*,
        from_status.id AS from_status_id,
        to_status.id AS to_status_id,
        from_status.behavior_bucket AS from_behavior_bucket,
        to_status.behavior_bucket AS to_behavior_bucket
    FROM required_transitions
    JOIN workflow_statuses from_status
      ON from_status.status_key = required_transitions.from_status_key
    JOIN workflow_statuses to_status
      ON to_status.status_key = required_transitions.to_status_key
),
updated AS (
    UPDATE workflow_transitions existing
    SET display_name = resolved.display_name,
        from_status = resolved.from_behavior_bucket,
        to_status = resolved.to_behavior_bucket,
        from_status_id = resolved.from_status_id,
        to_status_id = resolved.to_status_id,
        active = true,
        system_transition = false,
        protected_transition = false,
        sort_order = resolved.sort_order,
        updated_at = now()
    FROM resolved
    WHERE existing.action_key = resolved.action_key
      AND existing.from_status_id = resolved.from_status_id
      AND existing.to_status_id = resolved.to_status_id
    RETURNING existing.id
)
INSERT INTO workflow_transitions (
    action_key,
    display_name,
    from_status,
    to_status,
    from_status_id,
    to_status_id,
    active,
    system_transition,
    protected_transition,
    sort_order,
    created_at,
    updated_at
)
SELECT
    resolved.action_key,
    resolved.display_name,
    resolved.from_behavior_bucket,
    resolved.to_behavior_bucket,
    resolved.from_status_id,
    resolved.to_status_id,
    true,
    false,
    false,
    resolved.sort_order,
    now(),
    now()
FROM resolved
WHERE NOT EXISTS (
    SELECT 1
    FROM workflow_transitions existing
    WHERE existing.action_key = resolved.action_key
      AND existing.from_status_id = resolved.from_status_id
      AND existing.to_status_id = resolved.to_status_id
);

-- ============================================================
-- SECTION 2: Category and transition role scope.
-- Requires approved selected_category_key. This section still
-- does not activate the category.
-- ============================================================

-- Uncomment and set only after separate approval:
-- \set selected_category_key 'APPROVED_PRODUCTION_REPAIR_CATEGORY_KEY'

/*
WITH required_transitions(action_key, from_status_key, to_status_key) AS (
    VALUES
        ('START_REPAIR_WORK', 'NEW', 'IN_PROGRESS'),
        ('MARK_MISSING_PART', 'IN_PROGRESS', 'MISSING_PART'),
        ('NEED_CUSTOMER_APPROVAL', 'IN_PROGRESS', 'CUSTOMER_APPROVAL_PENDING'),
        ('MARK_IN_WARRANTY', 'IN_PROGRESS', 'IN_WARRANTY'),
        ('MARK_REPAIR_COMPLETED', 'IN_PROGRESS', 'REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR', 'IN_PROGRESS', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'IN_PROGRESS', 'CANCELLED_PENDING_DELIVERY'),
        ('MARK_PART_AVAILABLE', 'MISSING_PART', 'PART_AVAILABLE'),
        ('CUSTOMER_DECLINED_REPAIR', 'MISSING_PART', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'MISSING_PART', 'CANCELLED_PENDING_DELIVERY'),
        ('RESUME_WORK', 'PART_AVAILABLE', 'IN_PROGRESS'),
        ('MARK_REPAIR_COMPLETED', 'PART_AVAILABLE', 'REPAIR_COMPLETED'),
        ('MARK_MISSING_PART', 'PART_AVAILABLE', 'MISSING_PART'),
        ('CUSTOMER_DECLINED_REPAIR', 'PART_AVAILABLE', 'CUSTOMER_DECLINED'),
        ('CUSTOMER_APPROVED', 'CUSTOMER_APPROVAL_PENDING', 'IN_PROGRESS'),
        ('CUSTOMER_DECLINED_REPAIR', 'CUSTOMER_APPROVAL_PENDING', 'CUSTOMER_DECLINED'),
        ('LOG_WARRANTY_COMPLAINT', 'IN_WARRANTY', 'WARRANTY_COMPLAINT_LOGGED'),
        ('CUSTOMER_DECLINED_REPAIR', 'IN_WARRANTY', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'IN_WARRANTY', 'CANCELLED_PENDING_DELIVERY'),
        ('MARK_REPAIR_COMPLETED', 'WARRANTY_COMPLAINT_LOGGED', 'REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR', 'WARRANTY_COMPLAINT_LOGGED', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'WARRANTY_COMPLAINT_LOGGED', 'CANCELLED_PENDING_DELIVERY'),
        ('DELIVER_TO_CUSTOMER', 'REPAIR_COMPLETED', 'DELIVERED_TO_CUSTOMER'),
        ('DELIVER_TO_CUSTOMER', 'CUSTOMER_DECLINED', 'DELIVERED_TO_CUSTOMER'),
        ('DELIVER_TO_CUSTOMER', 'CANCELLED_PENDING_DELIVERY', 'DELIVERED_TO_CUSTOMER')
),
selected_category AS (
    SELECT id AS category_id
    FROM ticket_categories
    WHERE category_key = :'selected_category_key'
),
resolved_transitions AS (
    SELECT transition.id AS workflow_transition_id
    FROM required_transitions
    JOIN workflow_statuses from_status
      ON from_status.status_key = required_transitions.from_status_key
    JOIN workflow_statuses to_status
      ON to_status.status_key = required_transitions.to_status_key
    JOIN workflow_transitions transition
      ON transition.action_key = required_transitions.action_key
     AND transition.from_status_id = from_status.id
     AND transition.to_status_id = to_status.id
)
INSERT INTO workflow_transition_category_rules (
    workflow_transition_id,
    category_id,
    active,
    created_at,
    updated_at
)
SELECT
    resolved_transitions.workflow_transition_id,
    selected_category.category_id,
    true,
    now(),
    now()
FROM resolved_transitions
CROSS JOIN selected_category
ON CONFLICT (workflow_transition_id, category_id) DO UPDATE
SET active = true,
    updated_at = now();

WITH required_transitions(action_key, from_status_key, to_status_key) AS (
    VALUES
        ('START_REPAIR_WORK', 'NEW', 'IN_PROGRESS'),
        ('MARK_MISSING_PART', 'IN_PROGRESS', 'MISSING_PART'),
        ('NEED_CUSTOMER_APPROVAL', 'IN_PROGRESS', 'CUSTOMER_APPROVAL_PENDING'),
        ('MARK_IN_WARRANTY', 'IN_PROGRESS', 'IN_WARRANTY'),
        ('MARK_REPAIR_COMPLETED', 'IN_PROGRESS', 'REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR', 'IN_PROGRESS', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'IN_PROGRESS', 'CANCELLED_PENDING_DELIVERY'),
        ('MARK_PART_AVAILABLE', 'MISSING_PART', 'PART_AVAILABLE'),
        ('CUSTOMER_DECLINED_REPAIR', 'MISSING_PART', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'MISSING_PART', 'CANCELLED_PENDING_DELIVERY'),
        ('RESUME_WORK', 'PART_AVAILABLE', 'IN_PROGRESS'),
        ('MARK_REPAIR_COMPLETED', 'PART_AVAILABLE', 'REPAIR_COMPLETED'),
        ('MARK_MISSING_PART', 'PART_AVAILABLE', 'MISSING_PART'),
        ('CUSTOMER_DECLINED_REPAIR', 'PART_AVAILABLE', 'CUSTOMER_DECLINED'),
        ('CUSTOMER_APPROVED', 'CUSTOMER_APPROVAL_PENDING', 'IN_PROGRESS'),
        ('CUSTOMER_DECLINED_REPAIR', 'CUSTOMER_APPROVAL_PENDING', 'CUSTOMER_DECLINED'),
        ('LOG_WARRANTY_COMPLAINT', 'IN_WARRANTY', 'WARRANTY_COMPLAINT_LOGGED'),
        ('CUSTOMER_DECLINED_REPAIR', 'IN_WARRANTY', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'IN_WARRANTY', 'CANCELLED_PENDING_DELIVERY'),
        ('MARK_REPAIR_COMPLETED', 'WARRANTY_COMPLAINT_LOGGED', 'REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR', 'WARRANTY_COMPLAINT_LOGGED', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'WARRANTY_COMPLAINT_LOGGED', 'CANCELLED_PENDING_DELIVERY'),
        ('DELIVER_TO_CUSTOMER', 'REPAIR_COMPLETED', 'DELIVERED_TO_CUSTOMER'),
        ('DELIVER_TO_CUSTOMER', 'CUSTOMER_DECLINED', 'DELIVERED_TO_CUSTOMER'),
        ('DELIVER_TO_CUSTOMER', 'CANCELLED_PENDING_DELIVERY', 'DELIVERED_TO_CUSTOMER')
),
resolved_transitions AS (
    SELECT transition.id AS workflow_transition_id
    FROM required_transitions
    JOIN workflow_statuses from_status
      ON from_status.status_key = required_transitions.from_status_key
    JOIN workflow_statuses to_status
      ON to_status.status_key = required_transitions.to_status_key
    JOIN workflow_transitions transition
      ON transition.action_key = required_transitions.action_key
     AND transition.from_status_id = from_status.id
     AND transition.to_status_id = to_status.id
),
super_admin AS (
    SELECT id AS role_id
    FROM roles
    WHERE role_key = 'SUPER_ADMIN'
)
INSERT INTO workflow_transition_role_rules (
    workflow_transition_id,
    role_id,
    active,
    created_at,
    updated_at
)
SELECT
    resolved_transitions.workflow_transition_id,
    super_admin.role_id,
    true,
    now(),
    now()
FROM resolved_transitions
CROSS JOIN super_admin
ON CONFLICT (workflow_transition_id, role_id) DO UPDATE
SET active = true,
    updated_at = now();
*/

COMMIT;

-- ============================================================
-- VALIDATION QUERIES
-- Run after metadata migration, before any activation.
-- Expected: missing_* and duplicate queries return zero rows.
-- ============================================================

-- Required statuses exist and have the expected behavior.
WITH required_statuses(status_key, behavior_bucket, terminal) AS (
    VALUES
        ('MISSING_PART', 'IN_PROGRESS', false),
        ('PART_AVAILABLE', 'IN_PROGRESS', false),
        ('CUSTOMER_APPROVAL_PENDING', 'IN_PROGRESS', false),
        ('IN_WARRANTY', 'IN_PROGRESS', false),
        ('WARRANTY_COMPLAINT_LOGGED', 'IN_PROGRESS', false),
        ('REPAIR_COMPLETED', 'IN_PROGRESS', false),
        ('CUSTOMER_DECLINED', 'IN_PROGRESS', false),
        ('CANCELLED_PENDING_DELIVERY', 'IN_PROGRESS', false),
        ('DELIVERED_TO_CUSTOMER', 'COMPLETED', true)
)
SELECT required_statuses.*
FROM required_statuses
LEFT JOIN workflow_statuses existing
  ON existing.status_key = required_statuses.status_key
 AND existing.behavior_bucket = required_statuses.behavior_bucket
 AND existing.terminal = required_statuses.terminal
 AND existing.active = true
WHERE existing.id IS NULL;

-- Required actions exist and are active.
WITH required_actions(action_key) AS (
    VALUES
        ('START_REPAIR_WORK'),
        ('MARK_MISSING_PART'),
        ('MARK_PART_AVAILABLE'),
        ('RESUME_WORK'),
        ('NEED_CUSTOMER_APPROVAL'),
        ('CUSTOMER_APPROVED'),
        ('MARK_IN_WARRANTY'),
        ('LOG_WARRANTY_COMPLAINT'),
        ('MARK_REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR'),
        ('CANCEL_PENDING_DELIVERY'),
        ('DELIVER_TO_CUSTOMER')
)
SELECT required_actions.action_key
FROM required_actions
LEFT JOIN workflow_actions existing
  ON existing.action_key = required_actions.action_key
 AND existing.active = true
WHERE existing.id IS NULL;

-- All 25 required transitions exist and are active.
WITH required_transitions(action_key, from_status_key, to_status_key) AS (
    VALUES
        ('START_REPAIR_WORK', 'NEW', 'IN_PROGRESS'),
        ('MARK_MISSING_PART', 'IN_PROGRESS', 'MISSING_PART'),
        ('NEED_CUSTOMER_APPROVAL', 'IN_PROGRESS', 'CUSTOMER_APPROVAL_PENDING'),
        ('MARK_IN_WARRANTY', 'IN_PROGRESS', 'IN_WARRANTY'),
        ('MARK_REPAIR_COMPLETED', 'IN_PROGRESS', 'REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR', 'IN_PROGRESS', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'IN_PROGRESS', 'CANCELLED_PENDING_DELIVERY'),
        ('MARK_PART_AVAILABLE', 'MISSING_PART', 'PART_AVAILABLE'),
        ('CUSTOMER_DECLINED_REPAIR', 'MISSING_PART', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'MISSING_PART', 'CANCELLED_PENDING_DELIVERY'),
        ('RESUME_WORK', 'PART_AVAILABLE', 'IN_PROGRESS'),
        ('MARK_REPAIR_COMPLETED', 'PART_AVAILABLE', 'REPAIR_COMPLETED'),
        ('MARK_MISSING_PART', 'PART_AVAILABLE', 'MISSING_PART'),
        ('CUSTOMER_DECLINED_REPAIR', 'PART_AVAILABLE', 'CUSTOMER_DECLINED'),
        ('CUSTOMER_APPROVED', 'CUSTOMER_APPROVAL_PENDING', 'IN_PROGRESS'),
        ('CUSTOMER_DECLINED_REPAIR', 'CUSTOMER_APPROVAL_PENDING', 'CUSTOMER_DECLINED'),
        ('LOG_WARRANTY_COMPLAINT', 'IN_WARRANTY', 'WARRANTY_COMPLAINT_LOGGED'),
        ('CUSTOMER_DECLINED_REPAIR', 'IN_WARRANTY', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'IN_WARRANTY', 'CANCELLED_PENDING_DELIVERY'),
        ('MARK_REPAIR_COMPLETED', 'WARRANTY_COMPLAINT_LOGGED', 'REPAIR_COMPLETED'),
        ('CUSTOMER_DECLINED_REPAIR', 'WARRANTY_COMPLAINT_LOGGED', 'CUSTOMER_DECLINED'),
        ('CANCEL_PENDING_DELIVERY', 'WARRANTY_COMPLAINT_LOGGED', 'CANCELLED_PENDING_DELIVERY'),
        ('DELIVER_TO_CUSTOMER', 'REPAIR_COMPLETED', 'DELIVERED_TO_CUSTOMER'),
        ('DELIVER_TO_CUSTOMER', 'CUSTOMER_DECLINED', 'DELIVERED_TO_CUSTOMER'),
        ('DELIVER_TO_CUSTOMER', 'CANCELLED_PENDING_DELIVERY', 'DELIVERED_TO_CUSTOMER')
)
SELECT required_transitions.*
FROM required_transitions
JOIN workflow_statuses from_status
  ON from_status.status_key = required_transitions.from_status_key
JOIN workflow_statuses to_status
  ON to_status.status_key = required_transitions.to_status_key
LEFT JOIN workflow_transitions existing
  ON existing.action_key = required_transitions.action_key
 AND existing.from_status_id = from_status.id
 AND existing.to_status_id = to_status.id
 AND existing.active = true
WHERE existing.id IS NULL;

-- No duplicate active executable groups for repair action + from_status_id.
SELECT
    action_key,
    from_status_id,
    count(*) AS active_count
FROM workflow_transitions
WHERE active = true
  AND action_key IN (
      'START_REPAIR_WORK',
      'MARK_MISSING_PART',
      'MARK_PART_AVAILABLE',
      'RESUME_WORK',
      'NEED_CUSTOMER_APPROVAL',
      'CUSTOMER_APPROVED',
      'MARK_IN_WARRANTY',
      'LOG_WARRANTY_COMPLAINT',
      'MARK_REPAIR_COMPLETED',
      'CUSTOMER_DECLINED_REPAIR',
      'CANCEL_PENDING_DELIVERY',
      'DELIVER_TO_CUSTOMER'
  )
GROUP BY action_key, from_status_id
HAVING count(*) > 1;

-- SUPER_ADMIN has repair workflow transition rules; ADMIN/TECHNICIAN do not.
SELECT
    roles.role_key,
    count(*) AS active_repair_rule_count
FROM workflow_transition_role_rules role_rule
JOIN roles
  ON roles.id = role_rule.role_id
JOIN workflow_transitions transition
  ON transition.id = role_rule.workflow_transition_id
WHERE role_rule.active = true
  AND transition.action_key IN (
      'START_REPAIR_WORK',
      'MARK_MISSING_PART',
      'MARK_PART_AVAILABLE',
      'RESUME_WORK',
      'NEED_CUSTOMER_APPROVAL',
      'CUSTOMER_APPROVED',
      'MARK_IN_WARRANTY',
      'LOG_WARRANTY_COMPLAINT',
      'MARK_REPAIR_COMPLETED',
      'CUSTOMER_DECLINED_REPAIR',
      'CANCEL_PENDING_DELIVERY',
      'DELIVER_TO_CUSTOMER'
  )
GROUP BY roles.role_key
ORDER BY roles.role_key;

-- Production categories remain legacy.
SELECT
    category_key,
    workflow_mode,
    db_workflow_enabled,
    fixed_actions_enabled
FROM ticket_categories
WHERE workflow_mode <> 'LEGACY_FIXED'
   OR db_workflow_enabled <> false
   OR fixed_actions_enabled <> true;

-- Optional category-scope validation after selected_category_key is approved.
-- \set selected_category_key 'APPROVED_PRODUCTION_REPAIR_CATEGORY_KEY'
/*
WITH selected_category AS (
    SELECT id AS category_id
    FROM ticket_categories
    WHERE category_key = :'selected_category_key'
)
SELECT
    category.category_key,
    count(*) AS active_category_rule_count
FROM workflow_transition_category_rules category_rule
JOIN selected_category
  ON selected_category.category_id = category_rule.category_id
JOIN ticket_categories category
  ON category.id = category_rule.category_id
WHERE category_rule.active = true
GROUP BY category.category_key;
*/

-- ============================================================
-- ROLLBACK / DISABLE-ONLY SQL
-- Do not delete metadata or workflow history. Use only after an
-- approved selected_category_key has been scoped.
-- ============================================================

/*
BEGIN;

\set selected_category_key 'APPROVED_PRODUCTION_REPAIR_CATEGORY_KEY'

WITH selected_category AS (
    SELECT id AS category_id
    FROM ticket_categories
    WHERE category_key = :'selected_category_key'
),
repair_transitions AS (
    SELECT id AS workflow_transition_id
    FROM workflow_transitions
    WHERE action_key IN (
        'START_REPAIR_WORK',
        'MARK_MISSING_PART',
        'MARK_PART_AVAILABLE',
        'RESUME_WORK',
        'NEED_CUSTOMER_APPROVAL',
        'CUSTOMER_APPROVED',
        'MARK_IN_WARRANTY',
        'LOG_WARRANTY_COMPLAINT',
        'MARK_REPAIR_COMPLETED',
        'CUSTOMER_DECLINED_REPAIR',
        'CANCEL_PENDING_DELIVERY',
        'DELIVER_TO_CUSTOMER'
    )
)
UPDATE workflow_transition_category_rules category_rule
SET active = false,
    updated_at = now()
FROM selected_category, repair_transitions
WHERE category_rule.category_id = selected_category.category_id
  AND category_rule.workflow_transition_id = repair_transitions.workflow_transition_id;

UPDATE workflow_transition_role_rules role_rule
SET active = false,
    updated_at = now()
FROM roles, workflow_transitions transition
WHERE roles.id = role_rule.role_id
  AND transition.id = role_rule.workflow_transition_id
  AND roles.role_key = 'SUPER_ADMIN'
  AND transition.action_key IN (
      'START_REPAIR_WORK',
      'MARK_MISSING_PART',
      'MARK_PART_AVAILABLE',
      'RESUME_WORK',
      'NEED_CUSTOMER_APPROVAL',
      'CUSTOMER_APPROVED',
      'MARK_IN_WARRANTY',
      'LOG_WARRANTY_COMPLAINT',
      'MARK_REPAIR_COMPLETED',
      'CUSTOMER_DECLINED_REPAIR',
      'CANCEL_PENDING_DELIVERY',
      'DELIVER_TO_CUSTOMER'
  );

UPDATE ticket_categories
SET workflow_mode = 'LEGACY_FIXED',
    db_workflow_enabled = false,
    fixed_actions_enabled = true,
    workflow_mode_updated_at = now()
WHERE category_key = :'selected_category_key';

COMMIT;
*/
