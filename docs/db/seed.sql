-- =====================================================================
-- Enterprise Procurement System - Seed Data
-- Run AFTER schema.sql. Contains reference/config data only, no users
-- (the first ADMIN user is created in Milestone 4 with a proper password hash).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------
INSERT INTO roles (name, description) VALUES
    ('ADMIN',               'Manages users, roles, workflows and views audit logs'),
    ('EMPLOYEE',            'Creates and submits purchase requisitions'),
    ('MANAGER',             'First-level approver within a department'),
    ('DEPARTMENT_HEAD',     'Second-level approver within a department'),
    ('FINANCE_HEAD',        'Approves high-value requisitions; views spend reports'),
    ('PROCUREMENT_OFFICER', 'Manages suppliers and purchase orders');

-- ---------------------------------------------------------------------
-- Sample departments (head_user_id is filled in once users exist)
-- ---------------------------------------------------------------------
INSERT INTO departments (code, name, annual_budget) VALUES
    ('IT',  'Information Technology', 5000000.00),
    ('HR',  'Human Resources',        1500000.00),
    ('FIN', 'Finance',                1000000.00);

-- ---------------------------------------------------------------------
-- Sample procurement categories
-- ---------------------------------------------------------------------
INSERT INTO categories (code, name, description) VALUES
    ('IT-HW',  'IT Hardware',     'Laptops, desktops, servers, networking'),
    ('IT-SW',  'Software',        'Licences and subscriptions'),
    ('OFFICE', 'Office Supplies', 'Stationery, furniture, consumables'),
    ('SERV',   'Services',        'Consulting, maintenance, training');

-- ---------------------------------------------------------------------
-- Approval workflows (configurable multi-level authorization)
-- Rule: min_amount <= total_amount < max_amount
-- ---------------------------------------------------------------------
INSERT INTO approval_workflows (name, min_amount, max_amount) VALUES
    ('Low value',          0,      50000),
    ('Medium value',   50000,     500000),
    ('High value',    500000,       NULL);

INSERT INTO approval_workflow_steps (workflow_id, step_order, approver_role_id)
SELECT w.id, s.step_order, r.id
FROM (VALUES
        ('Low value',    1, 'MANAGER'),
        ('Medium value', 1, 'MANAGER'),
        ('Medium value', 2, 'DEPARTMENT_HEAD'),
        ('High value',   1, 'MANAGER'),
        ('High value',   2, 'DEPARTMENT_HEAD'),
        ('High value',   3, 'FINANCE_HEAD')
     ) AS s(workflow_name, step_order, role_name)
JOIN approval_workflows w ON w.name = s.workflow_name
JOIN roles r              ON r.name = s.role_name;
