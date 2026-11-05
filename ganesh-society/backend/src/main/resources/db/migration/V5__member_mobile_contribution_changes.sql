-- Existing members may have unknown mobile numbers; populate them before enforcing NOT NULL.
ALTER TABLE members ADD COLUMN mobile_number VARCHAR(10) NULL;
CREATE UNIQUE INDEX uk_members_mobile_number ON members(mobile_number);
CREATE TABLE contribution_changes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 contribution_id BIGINT NOT NULL,
 member_id BIGINT NOT NULL,
 old_month CHAR(7) NOT NULL,
 new_month CHAR(7) NOT NULL,
 old_amount DECIMAL(14,2) NOT NULL,
 new_amount DECIMAL(14,2) NOT NULL,
 changed_by VARCHAR(190) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_change_contribution(contribution_id),
 INDEX idx_change_member(member_id),
 CONSTRAINT fk_change_contribution FOREIGN KEY(contribution_id) REFERENCES contributions(id),
 CONSTRAINT fk_change_member FOREIGN KEY(member_id) REFERENCES members(id)
);
