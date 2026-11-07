-- Existing administrators remain valid; new administrators require mobile via API.
ALTER TABLE admin_users ADD COLUMN mobile_number VARCHAR(10) NULL;
CREATE UNIQUE INDEX uk_admin_users_mobile_number ON admin_users(mobile_number);
