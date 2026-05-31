--Initial User Creation
INSERT INTO employees (name, employee_Id, password, role)
VALUES (
    'Harsh Prajapati(SUPER_ADMIN)',
    'SUPER_ADMIN001',
    '$2a$10$mKW.pIeXm60F8D7u9G38Qu8.4ueYo.N87GNcfEWhNzvpn44Xis726',
    'SUPER_ADMIN'
);
INSERT INTO employees (name, employee_Id, password, role)
VALUES (
    'Harsh Prajapati(Admin)',
    'ADMIN_001',
    '$2a$10$mKW.pIeXm60F8D7u9G38Qu8.4ueYo.N87GNcfEWhNzvpn44Xis726',
    'ADMIN'
);
INSERT INTO employees (name, employee_Id, password, role)
VALUES (
    'Harsh Prajapati(Technician)',
    'TECHNICIAN_001',
    '$2a$10$mKW.pIeXm60F8D7u9G38Qu8.4ueYo.N87GNcfEWhNzvpn44Xis726',
    'TECHNICIAN'
);

--Ticket Number Sequence Creation
CREATE SEQUENCE IF NOT EXISTS ticket_number_seq
    START WITH 1
    INCREMENT BY 1;
