ALTER TABLE admin_users
    ADD COLUMN full_name VARCHAR(150) NULL, ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, ADD COLUMN last_login_at TIMESTAMP NULL;
CREATE TABLE roles
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE TABLE permissions
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL UNIQUE,
    module      VARCHAR(80)  NOT NULL,
    description VARCHAR(255)
);
CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES admin_users (id),
    FOREIGN KEY (role_id) REFERENCES roles (id)
);
CREATE TABLE role_permissions
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles (id),
    FOREIGN KEY (permission_id) REFERENCES permissions (id)
);
CREATE TABLE auth_tokens
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash CHAR(64)    NOT NULL UNIQUE,
    token_type VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES admin_users (id),
    INDEX      idx_auth_user(user_id)
);
CREATE TABLE audit_logs
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id   BIGINT NULL,
    action     VARCHAR(100) NOT NULL,
    target     VARCHAR(200),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_id) REFERENCES admin_users (id)
);
INSERT INTO roles(name, description)
VALUES ('SUPER_ADMIN', 'User and role administration'),
       ('ADMIN', 'Society operations'),
       ('VIEWER', 'Read-only administrative access');
INSERT INTO permissions(code, module)
VALUES ('DASHBOARD_VIEW', 'Dashboard'),
       ('MEMBER_VIEW', 'Members'),
       ('MEMBER_CREATE', 'Members'),
       ('MEMBER_UPDATE', 'Members'),
       ('CONTRIBUTION_VIEW', 'Contributions'),
       ('CONTRIBUTION_CREATE', 'Contributions'),
       ('LOAN_VIEW', 'Loans'),
       ('LOAN_CREATE', 'Loans'),
       ('LOAN_REPAYMENT_CREATE', 'Loans'),
       ('SETTINGS_VIEW', 'Settings'),
       ('SETTINGS_UPDATE', 'Settings'),
       ('USER_VIEW', 'Users'),
       ('USER_CREATE', 'Users'),
       ('USER_UPDATE', 'Users'),
       ('USER_DISABLE', 'Users'),
       ('ROLE_VIEW', 'Roles'),
       ('ROLE_MANAGE', 'Roles');
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN';
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN
      ('DASHBOARD_VIEW', 'MEMBER_VIEW', 'MEMBER_CREATE', 'MEMBER_UPDATE', 'CONTRIBUTION_VIEW', 'CONTRIBUTION_CREATE',
       'LOAN_VIEW', 'LOAN_CREATE', 'LOAN_REPAYMENT_CREATE', 'SETTINGS_VIEW', 'SETTINGS_UPDATE');
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'VIEWER'
  AND (p.code = 'DASHBOARD_VIEW' OR p.code LIKE '%_VIEW');
-- Existing admins retain society access; promote explicitly using secure one-time provisioning.
INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id
FROM admin_users u
         JOIN roles r ON r.name = 'ADMIN';
