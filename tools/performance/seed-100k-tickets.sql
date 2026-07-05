\set ON_ERROR_STOP on

\if :{?ticket_count}
\else
  \set ticket_count 100000
\endif

\if :{?delete_existing}
\else
  \set delete_existing 0
\endif

\if :{?ticket_prefix}
\else
  \set ticket_prefix 'KE-PERF-'
\endif

BEGIN;

INSERT INTO ticket_categories (
    category_key,
    display_name,
    active,
    system_category,
    sort_order,
    workflow_mode,
    fixed_actions_enabled,
    db_workflow_enabled,
    created_at,
    updated_at
)
VALUES (
    'PERF_TEST',
    'Performance Test',
    true,
    false,
    999,
    'LEGACY_FIXED',
    true,
    false,
    now(),
    now()
)
ON CONFLICT (category_key) DO UPDATE
SET active = true,
    display_name = EXCLUDED.display_name,
    updated_at = now();

INSERT INTO workflow_statuses (
    status_key,
    display_name,
    active,
    system_status,
    protected_status,
    terminal,
    behavior_bucket,
    sort_order,
    created_at,
    updated_at
)
VALUES
    ('NEW', 'New', true, true, true, false, 'NEW', 10, now(), now()),
    ('PICKED', 'Picked', true, true, true, false, 'PICKED', 20, now(), now()),
    ('IN_PROGRESS', 'In Progress', true, true, true, false, 'IN_PROGRESS', 30, now(), now()),
    ('COMPLETED', 'Completed', true, true, true, true, 'COMPLETED', 40, now(), now()),
    ('CANCELLED', 'Cancelled', true, true, true, true, 'CANCELLED', 50, now(), now())
ON CONFLICT (status_key) DO UPDATE
SET active = true,
    display_name = EXCLUDED.display_name,
    terminal = EXCLUDED.terminal,
    behavior_bucket = EXCLUDED.behavior_bucket,
    updated_at = now();

\if :delete_existing
DELETE FROM ticket_charge_items
WHERE ticket_id IN (
    SELECT id
    FROM tickets
    WHERE ticket_number LIKE :'ticket_prefix' || '%'
);

DELETE FROM ticket_workflow_history
WHERE ticket_number LIKE :'ticket_prefix' || '%'
   OR ticket_id IN (
       SELECT id
       FROM tickets
       WHERE ticket_number LIKE :'ticket_prefix' || '%'
   );

DELETE FROM ticket_dynamic_values
WHERE ticket_id IN (
    SELECT id
    FROM tickets
    WHERE ticket_number LIKE :'ticket_prefix' || '%'
);

DELETE FROM tickets
WHERE ticket_number LIKE :'ticket_prefix' || '%';
\endif

WITH constants AS (
    SELECT
        (SELECT id FROM ticket_categories WHERE category_key = 'PERF_TEST') AS category_id,
        (SELECT id FROM workflow_statuses WHERE status_key = 'NEW') AS new_status_id,
        (SELECT id FROM workflow_statuses WHERE status_key = 'PICKED') AS picked_status_id,
        (SELECT id FROM workflow_statuses WHERE status_key = 'IN_PROGRESS') AS in_progress_status_id,
        (SELECT id FROM workflow_statuses WHERE status_key = 'COMPLETED') AS completed_status_id,
        (SELECT id FROM workflow_statuses WHERE status_key = 'CANCELLED') AS cancelled_status_id,
        COALESCE(
            (SELECT employee_id FROM employees WHERE active = true ORDER BY id LIMIT 1),
            'PERF_SEED'
        ) AS actor_employee_id
),
series AS (
    SELECT generate_series(1, :ticket_count::int) AS n
),
ticket_rows AS (
    SELECT
        :'ticket_prefix' || lpad(n::text, 6, '0') AS ticket_number,
        'Perf Customer ' || lpad(((n % 5000) + 1)::text, 4, '0') AS customer_name,
        (9000000000 + (n % 25000))::text AS mobile_number,
        'Perf Area ' || lpad(((n % 750) + 1)::text, 3, '0') AS village_or_area,
        CASE n % 12
            WHEN 0 THEN 'Battery'
            WHEN 1 THEN 'Inverter'
            WHEN 2 THEN 'UPS'
            WHEN 3 THEN 'Fan'
            WHEN 4 THEN 'Stabilizer'
            WHEN 5 THEN 'Mixer'
            WHEN 6 THEN 'Wiring'
            WHEN 7 THEN 'Motor'
            WHEN 8 THEN 'LED TV'
            WHEN 9 THEN 'Water Pump'
            WHEN 10 THEN 'Cooler'
            ELSE 'Appliance'
        END AS product_type,
        CASE
            WHEN n % 20 = 0 THEN 'CANCELLED'
            WHEN n % 10 = 0 THEN 'COMPLETED'
            WHEN n % 4 = 0 THEN 'IN_PROGRESS'
            WHEN n % 3 = 0 THEN 'PICKED'
            ELSE 'NEW'
        END AS status,
        CASE
            WHEN n % 20 = 0 THEN constants.cancelled_status_id
            WHEN n % 10 = 0 THEN constants.completed_status_id
            WHEN n % 4 = 0 THEN constants.in_progress_status_id
            WHEN n % 3 = 0 THEN constants.picked_status_id
            ELSE constants.new_status_id
        END AS status_id,
        constants.category_id,
        'Synthetic performance test complaint #' || n AS complaint_description,
        now() - ((n % 730) || ' days')::interval - ((n % 86400) || ' seconds')::interval AS created_at,
        constants.actor_employee_id AS created_by_employee_id,
        CASE WHEN n % 3 = 0 THEN constants.actor_employee_id ELSE NULL END AS picked_by_employee_id
    FROM series
    CROSS JOIN constants
)
INSERT INTO tickets (
    ticket_number,
    customer_name,
    mobile_number,
    village_or_area,
    product_type,
    category_id,
    complaint_description,
    status,
    status_id,
    created_at,
    updated_at,
    created_by_employee_id,
    picked_by_employee_id,
    completed_at,
    completed_by_employee_id,
    completion_remark,
    cancelled_at,
    cancelled_by_employee_id,
    cancellation_reason,
    warranty_status,
    manufacturer_status
)
SELECT
    tr.ticket_number,
    tr.customer_name,
    tr.mobile_number,
    tr.village_or_area,
    tr.product_type,
    tr.category_id,
    tr.complaint_description,
    tr.status,
    tr.status_id,
    tr.created_at,
    tr.created_at + ((abs(hashtext(tr.ticket_number)) % 7200) || ' seconds')::interval,
    tr.created_by_employee_id,
    tr.picked_by_employee_id,
    CASE WHEN tr.status = 'COMPLETED' THEN tr.created_at + '2 days'::interval ELSE NULL END,
    CASE WHEN tr.status = 'COMPLETED' THEN tr.created_by_employee_id ELSE NULL END,
    CASE WHEN tr.status = 'COMPLETED' THEN 'Synthetic completion remark' ELSE NULL END,
    CASE WHEN tr.status = 'CANCELLED' THEN tr.created_at + '1 day'::interval ELSE NULL END,
    CASE WHEN tr.status = 'CANCELLED' THEN tr.created_by_employee_id ELSE NULL END,
    CASE WHEN tr.status = 'CANCELLED' THEN 'Synthetic cancellation reason' ELSE NULL END,
    'NOT_CHECKED',
    'NOT_REQUIRED'
FROM ticket_rows tr
ON CONFLICT (ticket_number) DO NOTHING;

WITH seeded AS (
    SELECT id, ticket_number, created_by_employee_id, created_at
    FROM tickets
    WHERE ticket_number LIKE :'ticket_prefix' || '%'
      AND abs(hashtext(ticket_number)) % 5 = 0
)
INSERT INTO ticket_charge_items (
    ticket_id,
    description,
    amount,
    created_at,
    created_by_employee_id
)
SELECT
    id,
    'Synthetic service charge',
    ((abs(hashtext(ticket_number)) % 4500) + 500)::numeric / 100,
    created_at + '3 hours'::interval,
    created_by_employee_id
FROM seeded
WHERE NOT EXISTS (
    SELECT 1
    FROM ticket_charge_items existing
    WHERE existing.ticket_id = seeded.id
      AND existing.description = 'Synthetic service charge'
);

WITH seeded AS (
    SELECT id, ticket_number, status, created_by_employee_id, created_at
    FROM tickets
    WHERE ticket_number LIKE :'ticket_prefix' || '%'
      AND abs(hashtext(ticket_number)) % 4 = 0
)
INSERT INTO ticket_workflow_history (
    ticket_id,
    ticket_number,
    action_key,
    from_status,
    to_status,
    executed_by_employee_id,
    executed_by_employee_name_snapshot,
    result,
    system_transition,
    custom_transition,
    created_at,
    metadata_json
)
SELECT
    id,
    ticket_number,
    'PERF_SEED',
    'NEW',
    status,
    created_by_employee_id,
    created_by_employee_id,
    'SUCCESS',
    true,
    false,
    created_at + '1 hour'::interval,
    '{"source":"performance-seed"}'
FROM seeded
WHERE NOT EXISTS (
    SELECT 1
    FROM ticket_workflow_history existing
    WHERE existing.ticket_id = seeded.id
      AND existing.action_key = 'PERF_SEED'
);

SELECT
    COUNT(*) AS performance_ticket_count,
    MIN(created_at) AS oldest_created_at,
    MAX(created_at) AS newest_created_at
FROM tickets
WHERE ticket_number LIKE :'ticket_prefix' || '%';

COMMIT;

ANALYZE tickets;
ANALYZE ticket_charge_items;
ANALYZE ticket_workflow_history;
