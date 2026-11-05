INSERT INTO permissions(code, module)
SELECT 'CONTRIBUTION_UPDATE', 'Contributions'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CONTRIBUTION_UPDATE');
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('SUPER_ADMIN','ADMIN') AND p.code = 'CONTRIBUTION_UPDATE'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
