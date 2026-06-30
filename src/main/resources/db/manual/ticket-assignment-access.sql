INSERT INTO access_key_metadata (
    access_key,
    display_name,
    description,
    category,
    active,
    system_key,
    protected_key,
    sort_order,
    created_at,
    updated_at
)
VALUES (
    'ASSIGN_TICKET',
    'Assign Ticket',
    'Assign a ticket current owner to an active employee.',
    'Ticket Actions',
    TRUE,
    TRUE,
    TRUE,
    50,
    now(),
    now()
)
ON CONFLICT (access_key) DO UPDATE
SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    active = TRUE,
    system_key = TRUE,
    protected_key = TRUE,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

INSERT INTO role_access_rules (role_id, access_key, allowed, created_at, updated_at)
SELECT r.id, 'ASSIGN_TICKET', TRUE, now(), now()
FROM roles r
WHERE r.role_key = 'ADMIN'
ON CONFLICT (role_id, access_key) DO UPDATE
SET allowed = TRUE, updated_at = now();
