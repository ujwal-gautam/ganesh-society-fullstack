-- Preserve financial history while hiding deleted entries from active totals and member history.
ALTER TABLE contributions
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deleted_by VARCHAR(255) NULL;

INSERT INTO permissions(code, module, description)
SELECT 'CONTRIBUTION_DELETE', 'Contributions', 'Soft delete a monthly contribution'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CONTRIBUTION_DELETE');

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.code = 'CONTRIBUTION_DELETE'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
