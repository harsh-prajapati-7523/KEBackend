--Initial User Creation
INSERT INTO employees (name, employee_Id, password, role)
VALUES (
    'Harsh Prajapati(SUPER_ADMIN)',
    'SUPER_ADMIN_001',
    '$2a$10$mKW.pIeXm60F8D7u9G38Qu8.4ueYo.N87GNcfEWhNzvpn44Xis726',
    'SUPER_ADMIN'
) ON CONFLICT (employee_id) DO NOTHING;;
INSERT INTO employees (name, employee_Id, password, role)
VALUES (
    'Harsh Prajapati(Admin)',
    'ADMIN_001',
    '$2a$10$mKW.pIeXm60F8D7u9G38Qu8.4ueYo.N87GNcfEWhNzvpn44Xis726',
    'ADMIN'
) ON CONFLICT (employee_id) DO NOTHING;;
INSERT INTO employees (name, employee_Id, password, role)
VALUES (
    'Harsh Prajapati(Technician)',
    'TECHNICIAN_001',
    '$2a$10$mKW.pIeXm60F8D7u9G38Qu8.4ueYo.N87GNcfEWhNzvpn44Xis726',
    'TECHNICIAN'
) ON CONFLICT (employee_id) DO NOTHING;;

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
