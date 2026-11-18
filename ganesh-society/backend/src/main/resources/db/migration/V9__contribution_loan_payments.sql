-- Keep monthly contribution and linked loan repayments in one transaction.
ALTER TABLE contributions
    ADD COLUMN loan_received DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN loan_interest DECIMAL(15,2) NOT NULL DEFAULT 0.00;
ALTER TABLE loan_repayments
    ADD COLUMN contribution_id BIGINT NULL;
CREATE INDEX idx_loan_repayments_contribution ON loan_repayments(contribution_id);
ALTER TABLE contribution_changes
    ADD COLUMN old_loan_received DECIMAL(15,2) NULL,
    ADD COLUMN new_loan_received DECIMAL(15,2) NULL,
    ADD COLUMN old_loan_interest DECIMAL(15,2) NULL,
    ADD COLUMN new_loan_interest DECIMAL(15,2) NULL;
