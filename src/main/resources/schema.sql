-- Employee bootstrap users are managed by BootstrapSuperAdminInitializer.
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_key VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE roles
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE roles
    ALTER COLUMN system_role SET DEFAULT FALSE;

ALTER TABLE roles
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE roles
    ALTER COLUMN updated_at SET DEFAULT now();

UPDATE roles
SET created_at = now()
WHERE created_at IS NULL;

UPDATE roles
SET updated_at = now()
WHERE updated_at IS NULL;

INSERT INTO roles (role_key, display_name, active, system_role, created_at, updated_at)
VALUES
    ('SUPER_ADMIN', 'Super Admin', TRUE, TRUE, now(), now()),
    ('ADMIN', 'Admin', TRUE, TRUE, now(), now()),
    ('TECHNICIAN', 'Technician', TRUE, TRUE, now(), now())
ON CONFLICT (role_key) DO UPDATE
SET
    display_name = EXCLUDED.display_name,
    active = CASE
        WHEN roles.role_key = 'SUPER_ADMIN' THEN TRUE
        ELSE roles.active
    END,
    system_role = TRUE,
    updated_at = now();

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS role_id BIGINT;

ALTER TABLE employees
    ALTER COLUMN role DROP NOT NULL;

UPDATE employees e
SET role_id = r.id
FROM roles r
WHERE e.role = r.role_key
  AND e.role_id IS NULL;

ALTER TABLE employees
    DROP CONSTRAINT IF EXISTS fk_employees_role;

ALTER TABLE employees
    ADD CONSTRAINT fk_employees_role FOREIGN KEY (role_id)
    REFERENCES roles(id);

ALTER TABLE employees
    DROP CONSTRAINT IF EXISTS ck_employees_role_backfilled;

ALTER TABLE employees
    ADD CONSTRAINT ck_employees_role_backfilled
    CHECK (role IS NULL OR role_id IS NOT NULL)
    NOT VALID;

ALTER TABLE employees
    VALIDATE CONSTRAINT ck_employees_role_backfilled;

CREATE TABLE IF NOT EXISTS role_access_rules (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    access_key VARCHAR(60) NOT NULL,
    allowed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by_employee_id BIGINT,
    CONSTRAINT fk_role_access_rules_role FOREIGN KEY (role_id)
        REFERENCES roles(id),
    CONSTRAINT fk_role_access_rules_updated_by_employee FOREIGN KEY (updated_by_employee_id)
        REFERENCES employees(id),
    CONSTRAINT uk_role_access_rules_role_access_key UNIQUE (role_id, access_key)
);

ALTER TABLE role_access_rules
    ALTER COLUMN allowed SET DEFAULT FALSE;

ALTER TABLE role_access_rules
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE role_access_rules
    ALTER COLUMN updated_at SET DEFAULT now();

ALTER TABLE role_access_rules
    DROP CONSTRAINT IF EXISTS ck_role_access_rules_access_key;

ALTER TABLE role_access_rules
    DROP CONSTRAINT IF EXISTS ck_role_access_rules_access_key_format;

ALTER TABLE role_access_rules
    ADD CONSTRAINT ck_role_access_rules_access_key_format
    CHECK (access_key ~ '^[A-Z0-9_]{2,60}$');

CREATE INDEX IF NOT EXISTS idx_role_access_rules_role_id ON role_access_rules(role_id);

CREATE INDEX IF NOT EXISTS idx_role_access_rules_access_key ON role_access_rules(access_key);

CREATE TABLE IF NOT EXISTS access_key_metadata (
    id BIGSERIAL PRIMARY KEY,
    access_key VARCHAR(60) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    category VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_key BOOLEAN NOT NULL DEFAULT TRUE,
    protected_key BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by_employee_id BIGINT,
    CONSTRAINT fk_access_key_metadata_updated_by_employee FOREIGN KEY (updated_by_employee_id)
        REFERENCES employees(id),
    CONSTRAINT ck_access_key_metadata_access_key_format CHECK (access_key ~ '^[A-Z0-9_]{2,60}$')
);

ALTER TABLE access_key_metadata
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE access_key_metadata
    ALTER COLUMN system_key SET DEFAULT TRUE;

ALTER TABLE access_key_metadata
    ALTER COLUMN protected_key SET DEFAULT TRUE;

ALTER TABLE access_key_metadata
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE access_key_metadata
    ALTER COLUMN updated_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_access_key_metadata_sort_key
    ON access_key_metadata(sort_order, access_key);

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
VALUES
    ('VIEW_DASHBOARD', 'View Dashboard', 'View the employee dashboard.', 'Dashboard', TRUE, TRUE, TRUE, 10, now(), now()),
    ('VIEW_TICKETS', 'View Tickets', 'View ticket lists and ticket details.', 'Tickets', TRUE, TRUE, TRUE, 20, now(), now()),
    ('CREATE_TICKET', 'Create Ticket', 'Create new tickets.', 'Tickets', TRUE, TRUE, TRUE, 30, now(), now()),
    ('PICK_TICKET', 'Pick Ticket', 'Pick or take ownership of an eligible ticket.', 'Ticket Actions', TRUE, TRUE, TRUE, 40, now(), now()),
    ('START_WORK', 'Start Work', 'Move an eligible ticket into work in progress.', 'Ticket Actions', TRUE, TRUE, TRUE, 50, now(), now()),
    ('COMPLETE_TICKET', 'Complete Ticket', 'Complete an eligible in-progress ticket.', 'Ticket Actions', TRUE, TRUE, TRUE, 60, now(), now()),
    ('CANCEL_TICKET', 'Cancel Ticket', 'Cancel an eligible ticket.', 'Ticket Actions', TRUE, TRUE, TRUE, 70, now(), now()),
    ('UPDATE_WARRANTY', 'Update Warranty', 'Update ticket warranty and manufacturer details.', 'Ticket Actions', TRUE, TRUE, TRUE, 80, now(), now()),
    ('VIEW_CUSTOMER_HISTORY', 'View Customer History', 'View customer ticket history.', 'Tickets', TRUE, TRUE, TRUE, 90, now(), now()),
    ('VIEW_CHARGES', 'View Charges', 'View ticket charges.', 'Ticket Charges', TRUE, TRUE, TRUE, 100, now(), now()),
    ('ADD_CHARGE', 'Add Charge', 'Add charges to eligible tickets.', 'Ticket Charges', TRUE, TRUE, TRUE, 110, now(), now()),
    ('DELETE_CHARGE', 'Delete Charge', 'Delete ticket charges.', 'Ticket Charges', TRUE, TRUE, TRUE, 120, now(), now()),
    ('USE_TICKET_SEARCH', 'Use Ticket Search', 'Use ticket search.', 'Ticket Filters', TRUE, TRUE, TRUE, 130, now(), now()),
    ('USE_TICKET_FILTERS', 'Use Ticket Filters', 'Use ticket filters.', 'Ticket Filters', TRUE, TRUE, TRUE, 140, now(), now()),
    ('USE_SMART_SUGGESTIONS', 'Use Smart Suggestions', 'Use smart suggestions in supported fields.', 'Smart Suggestions', TRUE, TRUE, TRUE, 150, now(), now()),
    ('VIEW_EMPLOYEE_MANAGEMENT', 'View Employee Management', 'View employee management.', 'Admin Configuration', TRUE, TRUE, TRUE, 160, now(), now()),
    ('MANAGE_EMPLOYEES', 'Manage Employees', 'Create, update, and administer employees.', 'Admin Configuration', TRUE, TRUE, TRUE, 170, now(), now()),
    ('VIEW_ROLE_MANAGEMENT', 'View Role Management', 'View role management.', 'Role Access', TRUE, TRUE, TRUE, 180, now(), now()),
    ('MANAGE_ROLES', 'Manage Roles', 'Create, update, and administer roles.', 'Role Access', TRUE, TRUE, TRUE, 190, now(), now()),
    ('VIEW_TICKET_CATEGORY_MANAGEMENT', 'View Ticket Category Management', 'View ticket category management.', 'Admin Configuration', TRUE, TRUE, TRUE, 200, now(), now()),
    ('MANAGE_TICKET_CATEGORIES', 'Manage Ticket Categories', 'Create and update ticket categories.', 'Admin Configuration', TRUE, TRUE, TRUE, 210, now(), now()),
    ('VIEW_TICKET_FIELD_MANAGEMENT', 'View Ticket Field Management', 'View ticket field management.', 'Admin Configuration', TRUE, TRUE, TRUE, 220, now(), now()),
    ('MANAGE_TICKET_FIELDS', 'Manage Ticket Fields', 'Create and update ticket fields.', 'Admin Configuration', TRUE, TRUE, TRUE, 230, now(), now()),
    ('VIEW_CATEGORY_FIELD_CONFIGURATION', 'View Category Field Configuration', 'View category field configuration.', 'Admin Configuration', TRUE, TRUE, TRUE, 240, now(), now()),
    ('MANAGE_CATEGORY_FIELD_CONFIGS', 'Manage Category Field Configs', 'Create and update category field configuration.', 'Admin Configuration', TRUE, TRUE, TRUE, 250, now(), now()),
    ('VIEW_DROPDOWN_SOURCE_MANAGEMENT', 'View Dropdown Source Management', 'View dropdown source management.', 'Admin Configuration', TRUE, TRUE, TRUE, 260, now(), now()),
    ('MANAGE_DROPDOWN_SOURCES', 'Manage Dropdown Sources', 'Create and update dropdown sources.', 'Admin Configuration', TRUE, TRUE, TRUE, 270, now(), now())
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
SELECT r.id, v.access_key, TRUE, now(), now()
FROM roles r
CROSS JOIN (
    VALUES
        ('VIEW_DASHBOARD'),
        ('VIEW_TICKETS'),
        ('CREATE_TICKET'),
        ('PICK_TICKET'),
        ('START_WORK'),
        ('COMPLETE_TICKET'),
        ('CANCEL_TICKET'),
        ('UPDATE_WARRANTY'),
        ('VIEW_CUSTOMER_HISTORY'),
        ('VIEW_CHARGES'),
        ('ADD_CHARGE'),
        ('DELETE_CHARGE'),
        ('USE_TICKET_SEARCH'),
        ('USE_TICKET_FILTERS'),
        ('USE_SMART_SUGGESTIONS')
) AS v(access_key)
WHERE r.role_key = 'ADMIN'
ON CONFLICT (role_id, access_key) DO NOTHING;

INSERT INTO role_access_rules (role_id, access_key, allowed, created_at, updated_at)
SELECT r.id, v.access_key, TRUE, now(), now()
FROM roles r
CROSS JOIN (
    VALUES
        ('VIEW_DASHBOARD'),
        ('VIEW_TICKETS'),
        ('PICK_TICKET'),
        ('START_WORK'),
        ('COMPLETE_TICKET'),
        ('UPDATE_WARRANTY'),
        ('VIEW_CUSTOMER_HISTORY'),
        ('VIEW_CHARGES'),
        ('ADD_CHARGE'),
        ('USE_TICKET_SEARCH'),
        ('USE_TICKET_FILTERS'),
        ('USE_SMART_SUGGESTIONS')
) AS v(access_key)
WHERE r.role_key = 'TECHNICIAN'
ON CONFLICT (role_id, access_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS workflow_transitions (
    id BIGSERIAL PRIMARY KEY,
    action_key VARCHAR(60) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER,
    system_transition BOOLEAN NOT NULL DEFAULT TRUE,
    protected_transition BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by_employee_id BIGINT,
    CONSTRAINT fk_workflow_transitions_updated_by_employee FOREIGN KEY (updated_by_employee_id)
        REFERENCES employees(id),
    CONSTRAINT uk_workflow_transitions_action_from_to UNIQUE (action_key, from_status, to_status),
    CONSTRAINT ck_workflow_transitions_action_key CHECK (action_key IN (
        'PICK_TICKET',
        'START_WORK',
        'COMPLETE_TICKET',
        'CANCEL_TICKET'
    )),
    CONSTRAINT ck_workflow_transitions_from_status CHECK (from_status IN (
        'NEW',
        'PICKED',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED'
    )),
    CONSTRAINT ck_workflow_transitions_to_status CHECK (to_status IN (
        'NEW',
        'PICKED',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED'
    )),
    CONSTRAINT ck_workflow_transitions_terminal_from_status CHECK (from_status NOT IN ('COMPLETED', 'CANCELLED'))
);

ALTER TABLE workflow_transitions
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE workflow_transitions
    ALTER COLUMN system_transition SET DEFAULT TRUE;

ALTER TABLE workflow_transitions
    ALTER COLUMN protected_transition SET DEFAULT TRUE;

ALTER TABLE workflow_transitions
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE workflow_transitions
    ALTER COLUMN updated_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_workflow_transitions_action_from_active
    ON workflow_transitions(action_key, from_status, active);

INSERT INTO workflow_transitions (
    action_key,
    display_name,
    from_status,
    to_status,
    active,
    sort_order,
    system_transition,
    protected_transition,
    created_at,
    updated_at
)
VALUES
    ('PICK_TICKET', 'Pick Ticket', 'NEW', 'PICKED', TRUE, 10, TRUE, TRUE, now(), now()),
    ('PICK_TICKET', 'Pick Ticket', 'PICKED', 'PICKED', TRUE, 20, TRUE, TRUE, now(), now()),
    ('START_WORK', 'Start Work', 'PICKED', 'IN_PROGRESS', TRUE, 30, TRUE, TRUE, now(), now()),
    ('COMPLETE_TICKET', 'Complete Ticket', 'IN_PROGRESS', 'COMPLETED', TRUE, 40, TRUE, TRUE, now(), now()),
    ('CANCEL_TICKET', 'Cancel Ticket', 'NEW', 'CANCELLED', TRUE, 50, TRUE, TRUE, now(), now()),
    ('CANCEL_TICKET', 'Cancel Ticket', 'PICKED', 'CANCELLED', TRUE, 60, TRUE, TRUE, now(), now()),
    ('CANCEL_TICKET', 'Cancel Ticket', 'IN_PROGRESS', 'CANCELLED', TRUE, 70, TRUE, TRUE, now(), now())
ON CONFLICT (action_key, from_status, to_status) DO UPDATE
SET
    display_name = EXCLUDED.display_name,
    sort_order = EXCLUDED.sort_order,
    system_transition = TRUE,
    protected_transition = TRUE,
    updated_at = now();

CREATE TABLE IF NOT EXISTS workflow_actions (
    id BIGSERIAL PRIMARY KEY,
    action_key VARCHAR(60) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    button_label VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_action BOOLEAN NOT NULL DEFAULT FALSE,
    protected_action BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER,
    requires_comment BOOLEAN NOT NULL DEFAULT FALSE,
    confirmation_required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by_employee_id BIGINT,
    CONSTRAINT fk_workflow_actions_updated_by_employee FOREIGN KEY (updated_by_employee_id)
        REFERENCES employees(id),
    CONSTRAINT ck_workflow_actions_action_key_format CHECK (action_key ~ '^[A-Z0-9_]{2,60}$')
);

ALTER TABLE workflow_actions
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE workflow_actions
    ALTER COLUMN system_action SET DEFAULT FALSE;

ALTER TABLE workflow_actions
    ALTER COLUMN protected_action SET DEFAULT FALSE;

ALTER TABLE workflow_actions
    ALTER COLUMN requires_comment SET DEFAULT FALSE;

ALTER TABLE workflow_actions
    ALTER COLUMN confirmation_required SET DEFAULT FALSE;

ALTER TABLE workflow_actions
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE workflow_actions
    ALTER COLUMN updated_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_workflow_actions_sort_key
    ON workflow_actions(sort_order, action_key);

INSERT INTO workflow_actions (
    action_key,
    display_name,
    button_label,
    description,
    active,
    system_action,
    protected_action,
    sort_order,
    requires_comment,
    confirmation_required,
    created_at,
    updated_at
)
VALUES
    ('PICK_TICKET', 'Pick Ticket', 'Pick Ticket', 'Pick or take ownership of a new/picked ticket.', TRUE, TRUE, TRUE, 10, FALSE, FALSE, now(), now()),
    ('START_WORK', 'Start Work', 'Start Work', 'Move a picked ticket into work in progress.', TRUE, TRUE, TRUE, 20, FALSE, FALSE, now(), now()),
    ('COMPLETE_TICKET', 'Complete Ticket', 'Complete Ticket', 'Complete an in-progress ticket after required checks.', TRUE, TRUE, TRUE, 30, FALSE, FALSE, now(), now()),
    ('CANCEL_TICKET', 'Cancel Ticket', 'Cancel Ticket', 'Cancel an eligible ticket.', TRUE, TRUE, TRUE, 40, FALSE, FALSE, now(), now())
ON CONFLICT (action_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS workflow_statuses (
    id BIGSERIAL PRIMARY KEY,
    status_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_status BOOLEAN NOT NULL DEFAULT FALSE,
    protected_status BOOLEAN NOT NULL DEFAULT FALSE,
    terminal BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by_employee_id BIGINT,
    CONSTRAINT fk_workflow_statuses_updated_by_employee FOREIGN KEY (updated_by_employee_id)
        REFERENCES employees(id)
);

ALTER TABLE workflow_statuses
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE workflow_statuses
    ALTER COLUMN system_status SET DEFAULT FALSE;

ALTER TABLE workflow_statuses
    ALTER COLUMN protected_status SET DEFAULT FALSE;

ALTER TABLE workflow_statuses
    ALTER COLUMN terminal SET DEFAULT FALSE;

ALTER TABLE workflow_statuses
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE workflow_statuses
    ALTER COLUMN updated_at SET DEFAULT now();

ALTER TABLE workflow_statuses
    DROP CONSTRAINT IF EXISTS ck_workflow_statuses_status_key_format;

ALTER TABLE workflow_statuses
    ADD CONSTRAINT ck_workflow_statuses_status_key_format
    CHECK (status_key ~ '^[A-Z0-9_]{2,50}$');

ALTER TABLE workflow_statuses
    ADD COLUMN IF NOT EXISTS behavior_bucket VARCHAR(30);

ALTER TABLE workflow_statuses
    DROP CONSTRAINT IF EXISTS ck_workflow_statuses_behavior_bucket;

ALTER TABLE workflow_statuses
    ADD CONSTRAINT ck_workflow_statuses_behavior_bucket
    CHECK (behavior_bucket IS NULL OR behavior_bucket IN (
        'NEW',
        'PICKED',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED'
    ));

CREATE INDEX IF NOT EXISTS idx_workflow_statuses_active ON workflow_statuses(active);

CREATE INDEX IF NOT EXISTS idx_workflow_statuses_sort_order ON workflow_statuses(sort_order);

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
    ('NEW', 'New', TRUE, TRUE, TRUE, FALSE, 'NEW', 10, now(), now()),
    ('PICKED', 'Picked', TRUE, TRUE, TRUE, FALSE, 'PICKED', 20, now(), now()),
    ('IN_PROGRESS', 'In Progress', TRUE, TRUE, TRUE, FALSE, 'IN_PROGRESS', 30, now(), now()),
    ('COMPLETED', 'Completed', TRUE, TRUE, TRUE, TRUE, 'COMPLETED', 40, now(), now()),
    ('CANCELLED', 'Cancelled', TRUE, TRUE, TRUE, TRUE, 'CANCELLED', 50, now(), now())
ON CONFLICT (status_key) DO UPDATE
SET
    display_name = EXCLUDED.display_name,
    active = TRUE,
    system_status = TRUE,
    protected_status = TRUE,
    terminal = EXCLUDED.terminal,
    behavior_bucket = EXCLUDED.behavior_bucket,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

ALTER TABLE workflow_transitions
    ADD COLUMN IF NOT EXISTS from_status_id BIGINT;

ALTER TABLE workflow_transitions
    ADD COLUMN IF NOT EXISTS to_status_id BIGINT;

UPDATE workflow_transitions wt
SET from_status_id = ws.id
FROM workflow_statuses ws
WHERE wt.from_status_id IS NULL
  AND wt.from_status = ws.status_key
  AND ws.status_key IN ('NEW', 'PICKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
  AND ws.system_status = TRUE
  AND ws.protected_status = TRUE;

UPDATE workflow_transitions wt
SET to_status_id = ws.id
FROM workflow_statuses ws
WHERE wt.to_status_id IS NULL
  AND wt.to_status = ws.status_key
  AND ws.status_key IN ('NEW', 'PICKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
  AND ws.system_status = TRUE
  AND ws.protected_status = TRUE;

ALTER TABLE workflow_transitions
    DROP CONSTRAINT IF EXISTS fk_workflow_transitions_from_status;

ALTER TABLE workflow_transitions
    ADD CONSTRAINT fk_workflow_transitions_from_status FOREIGN KEY (from_status_id)
    REFERENCES workflow_statuses(id);

ALTER TABLE workflow_transitions
    DROP CONSTRAINT IF EXISTS fk_workflow_transitions_to_status;

ALTER TABLE workflow_transitions
    ADD CONSTRAINT fk_workflow_transitions_to_status FOREIGN KEY (to_status_id)
    REFERENCES workflow_statuses(id);

CREATE INDEX IF NOT EXISTS idx_workflow_transitions_from_status_id
    ON workflow_transitions(from_status_id);

CREATE INDEX IF NOT EXISTS idx_workflow_transitions_to_status_id
    ON workflow_transitions(to_status_id);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS status_id BIGINT;

UPDATE tickets t
SET status_id = ws.id
FROM workflow_statuses ws
WHERE t.status_id IS NULL
  AND t.status = ws.status_key
  AND ws.status_key IN ('NEW', 'PICKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
  AND ws.system_status = TRUE
  AND ws.protected_status = TRUE
  AND ws.active = TRUE;

ALTER TABLE tickets
    DROP CONSTRAINT IF EXISTS fk_tickets_status;

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_status FOREIGN KEY (status_id)
    REFERENCES workflow_statuses(id);

CREATE INDEX IF NOT EXISTS idx_tickets_status_id ON tickets(status_id);

CREATE TABLE IF NOT EXISTS ticket_categories (
    id BIGSERIAL PRIMARY KEY,
    category_key VARCHAR(40) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_category BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE ticket_categories
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE ticket_categories
    ALTER COLUMN system_category SET DEFAULT FALSE;

ALTER TABLE ticket_categories
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE ticket_categories
    ALTER COLUMN updated_at SET DEFAULT now();

UPDATE ticket_categories
SET created_at = now()
WHERE created_at IS NULL;

UPDATE ticket_categories
SET updated_at = now()
WHERE updated_at IS NULL;

INSERT INTO ticket_categories (category_key, display_name, active, system_category, sort_order, created_at, updated_at)
VALUES
    ('INSTALLATION', 'Installation', TRUE, TRUE, 10, now(), now()),
    ('BATTERY_RECHARGE', 'Battery Recharge', TRUE, TRUE, 20, now(), now()),
    ('ELECTRICAL_REPAIR', 'Electrical Repair', TRUE, TRUE, 30, now(), now()),
    ('OTHER', 'Other', TRUE, TRUE, 40, now(), now())
ON CONFLICT (category_key) DO UPDATE
SET
    display_name = EXCLUDED.display_name,
    active = CASE
        WHEN ticket_categories.category_key = 'OTHER' THEN TRUE
        ELSE ticket_categories.active
    END,
    system_category = TRUE,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

CREATE INDEX IF NOT EXISTS idx_ticket_categories_active ON ticket_categories(active);

CREATE INDEX IF NOT EXISTS idx_ticket_categories_sort_order ON ticket_categories(sort_order);

CREATE TABLE IF NOT EXISTS dropdown_sources (
    id BIGSERIAL PRIMARY KEY,
    source_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_source BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE dropdown_sources
    ALTER COLUMN source_type SET DEFAULT 'MANUAL';

ALTER TABLE dropdown_sources
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE dropdown_sources
    ALTER COLUMN system_source SET DEFAULT FALSE;

ALTER TABLE dropdown_sources
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE dropdown_sources
    ALTER COLUMN updated_at SET DEFAULT now();

ALTER TABLE dropdown_sources
    DROP CONSTRAINT IF EXISTS ck_dropdown_sources_source_key_format;

ALTER TABLE dropdown_sources
    ADD CONSTRAINT ck_dropdown_sources_source_key_format
    CHECK (source_key ~ '^[A-Z0-9_]{3,50}$');

ALTER TABLE dropdown_sources
    DROP CONSTRAINT IF EXISTS ck_dropdown_sources_source_type;

ALTER TABLE dropdown_sources
    ADD CONSTRAINT ck_dropdown_sources_source_type
    CHECK (source_type IN ('MANUAL'));

CREATE INDEX IF NOT EXISTS idx_dropdown_sources_active ON dropdown_sources(active);

CREATE TABLE IF NOT EXISTS dropdown_options (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL,
    option_key VARCHAR(50) NOT NULL,
    display_value VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_dropdown_options_source FOREIGN KEY (source_id)
        REFERENCES dropdown_sources(id),
    CONSTRAINT uk_dropdown_options_source_option_key UNIQUE (source_id, option_key)
);

ALTER TABLE dropdown_options
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE dropdown_options
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE dropdown_options
    ALTER COLUMN updated_at SET DEFAULT now();

ALTER TABLE dropdown_options
    DROP CONSTRAINT IF EXISTS ck_dropdown_options_option_key_format;

ALTER TABLE dropdown_options
    ADD CONSTRAINT ck_dropdown_options_option_key_format
    CHECK (option_key ~ '^[A-Z0-9_]{2,50}$');

CREATE INDEX IF NOT EXISTS idx_dropdown_options_source_id ON dropdown_options(source_id);

CREATE INDEX IF NOT EXISTS idx_dropdown_options_active ON dropdown_options(active);

CREATE INDEX IF NOT EXISTS idx_dropdown_options_sort_order ON dropdown_options(sort_order);

CREATE TABLE IF NOT EXISTS ticket_field_definitions (
    id BIGSERIAL PRIMARY KEY,
    field_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_field BOOLEAN NOT NULL DEFAULT FALSE,
    help_text VARCHAR(255),
    default_required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE ticket_field_definitions
    ALTER COLUMN active SET DEFAULT TRUE;

ALTER TABLE ticket_field_definitions
    ALTER COLUMN system_field SET DEFAULT FALSE;

ALTER TABLE ticket_field_definitions
    ALTER COLUMN default_required SET DEFAULT FALSE;

ALTER TABLE ticket_field_definitions
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE ticket_field_definitions
    ALTER COLUMN updated_at SET DEFAULT now();

ALTER TABLE ticket_field_definitions
    ADD COLUMN IF NOT EXISTS dropdown_source_id BIGINT;

ALTER TABLE ticket_field_definitions
    DROP CONSTRAINT IF EXISTS fk_ticket_field_definitions_dropdown_source;

ALTER TABLE ticket_field_definitions
    ADD CONSTRAINT fk_ticket_field_definitions_dropdown_source FOREIGN KEY (dropdown_source_id)
    REFERENCES dropdown_sources(id);

ALTER TABLE ticket_field_definitions
    DROP CONSTRAINT IF EXISTS ck_ticket_field_definitions_field_key_format;

ALTER TABLE ticket_field_definitions
    ADD CONSTRAINT ck_ticket_field_definitions_field_key_format
    CHECK (field_key ~ '^[A-Z0-9_]{3,50}$');

ALTER TABLE ticket_field_definitions
    DROP CONSTRAINT IF EXISTS ck_ticket_field_definitions_field_type;

ALTER TABLE ticket_field_definitions
    ADD CONSTRAINT ck_ticket_field_definitions_field_type
    CHECK (field_type IN ('TEXT', 'NUMBER', 'DROPDOWN', 'TEXTAREA'));

ALTER TABLE ticket_field_definitions
    DROP CONSTRAINT IF EXISTS ck_ticket_field_definitions_dropdown_source_type;

ALTER TABLE ticket_field_definitions
    ADD CONSTRAINT ck_ticket_field_definitions_dropdown_source_type
    CHECK (field_type = 'DROPDOWN' OR dropdown_source_id IS NULL);

UPDATE ticket_field_definitions
SET created_at = now()
WHERE created_at IS NULL;

UPDATE ticket_field_definitions
SET updated_at = now()
WHERE updated_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ticket_field_definitions_active ON ticket_field_definitions(active);

CREATE INDEX IF NOT EXISTS idx_ticket_field_definitions_field_type ON ticket_field_definitions(field_type);

CREATE INDEX IF NOT EXISTS idx_ticket_field_definitions_sort_order ON ticket_field_definitions(sort_order);

CREATE INDEX IF NOT EXISTS idx_ticket_field_definitions_dropdown_source_id ON ticket_field_definitions(dropdown_source_id);

CREATE TABLE IF NOT EXISTS category_field_configs (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_category_field_configs_category FOREIGN KEY (category_id)
        REFERENCES ticket_categories(id),
    CONSTRAINT fk_category_field_configs_field_definition FOREIGN KEY (field_definition_id)
        REFERENCES ticket_field_definitions(id),
    CONSTRAINT uk_category_field_configs_category_field UNIQUE (category_id, field_definition_id)
);

ALTER TABLE category_field_configs
    ALTER COLUMN required SET DEFAULT FALSE;

ALTER TABLE category_field_configs
    ALTER COLUMN visible SET DEFAULT TRUE;

ALTER TABLE category_field_configs
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE category_field_configs
    ALTER COLUMN updated_at SET DEFAULT now();

UPDATE category_field_configs
SET created_at = now()
WHERE created_at IS NULL;

UPDATE category_field_configs
SET updated_at = now()
WHERE updated_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_category_field_configs_category_id ON category_field_configs(category_id);

CREATE INDEX IF NOT EXISTS idx_category_field_configs_field_definition_id ON category_field_configs(field_definition_id);

CREATE INDEX IF NOT EXISTS idx_category_field_configs_sort_order ON category_field_configs(sort_order);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS category_id BIGINT;

ALTER TABLE tickets
    ALTER COLUMN category DROP NOT NULL;

UPDATE tickets t
SET category_id = c.id
FROM ticket_categories c
WHERE t.category = c.category_key
  AND t.category_id IS NULL;

ALTER TABLE tickets
    DROP CONSTRAINT IF EXISTS fk_tickets_category;

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_category FOREIGN KEY (category_id)
    REFERENCES ticket_categories(id);

ALTER TABLE tickets
    DROP CONSTRAINT IF EXISTS ck_tickets_category_backfilled;

ALTER TABLE tickets
    ADD CONSTRAINT ck_tickets_category_backfilled
    CHECK (category IS NULL OR category_id IS NOT NULL)
    NOT VALID;

ALTER TABLE tickets
    VALIDATE CONSTRAINT ck_tickets_category_backfilled;

CREATE INDEX IF NOT EXISTS idx_tickets_category_id ON tickets(category_id);

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

CREATE TABLE IF NOT EXISTS ticket_dynamic_values (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    category_field_config_id BIGINT NOT NULL,
    field_key_snapshot VARCHAR(50) NOT NULL,
    field_label_snapshot VARCHAR(80) NOT NULL,
    field_type_snapshot VARCHAR(20) NOT NULL,
    value_text VARCHAR(1000),
    value_number NUMERIC(12,2),
    display_value VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_ticket_dynamic_values_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ticket_dynamic_values_field_definition FOREIGN KEY (field_definition_id)
        REFERENCES ticket_field_definitions(id),
    CONSTRAINT fk_ticket_dynamic_values_category_field_config FOREIGN KEY (category_field_config_id)
        REFERENCES category_field_configs(id),
    CONSTRAINT uk_ticket_dynamic_values_ticket_field UNIQUE (ticket_id, field_definition_id)
);

ALTER TABLE ticket_dynamic_values
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE ticket_dynamic_values
    ALTER COLUMN updated_at SET DEFAULT now();

ALTER TABLE ticket_dynamic_values
    DROP CONSTRAINT IF EXISTS ck_ticket_dynamic_values_field_type_snapshot;

ALTER TABLE ticket_dynamic_values
    ADD CONSTRAINT ck_ticket_dynamic_values_field_type_snapshot
    CHECK (field_type_snapshot IN ('TEXT', 'NUMBER', 'DROPDOWN', 'TEXTAREA'));

CREATE INDEX IF NOT EXISTS idx_ticket_dynamic_values_ticket_id ON ticket_dynamic_values(ticket_id);

CREATE INDEX IF NOT EXISTS idx_ticket_dynamic_values_field_definition_id ON ticket_dynamic_values(field_definition_id);

CREATE INDEX IF NOT EXISTS idx_ticket_dynamic_values_category_field_config_id ON ticket_dynamic_values(category_field_config_id);
