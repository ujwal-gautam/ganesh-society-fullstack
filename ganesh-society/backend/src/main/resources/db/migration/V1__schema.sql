CREATE TABLE society_settings (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 group_name VARCHAR(120) NOT NULL,
 start_month CHAR(7) NOT NULL,
 cycle_months INT NOT NULL DEFAULT 12,
 monthly_amount_per_share DECIMAL(12,2) NOT NULL DEFAULT 1000.00,
 monthly_interest_rate DECIMAL(6,3) NOT NULL DEFAULT 3.000,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT chk_cycle CHECK(cycle_months BETWEEN 1 AND 120),
 CONSTRAINT chk_rate CHECK(monthly_interest_rate BETWEEN 0 AND 100)
);
CREATE TABLE admin_users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 email VARCHAR(190) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE members (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 full_name VARCHAR(150) NOT NULL,
 shares INT NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 joined_on DATE NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_shares CHECK(shares > 0)
);
CREATE TABLE contributions (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 member_id BIGINT NOT NULL,
 contribution_month CHAR(7) NOT NULL,
 amount DECIMAL(14,2) NOT NULL,
 paid_on DATE NOT NULL,
 notes VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_contribution_member FOREIGN KEY(member_id) REFERENCES members(id),
 CONSTRAINT chk_contribution_amount CHECK(amount > 0),
 INDEX idx_contribution_member_month(member_id, contribution_month)
);
CREATE TABLE loans (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 member_id BIGINT NOT NULL,
 principal DECIMAL(14,2) NOT NULL,
 term_months INT NOT NULL,
 issued_on DATE NOT NULL,
 needs_review BOOLEAN NOT NULL DEFAULT FALSE,
 notes VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_loan_member FOREIGN KEY(member_id) REFERENCES members(id),
 CONSTRAINT chk_loan_principal CHECK(principal > 0),
 CONSTRAINT chk_term CHECK(term_months > 0),
 INDEX idx_loan_member(member_id)
);
CREATE TABLE loan_repayments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 loan_id BIGINT NOT NULL,
 paid_on DATE NOT NULL,
 received_amount DECIMAL(14,2) NOT NULL,
 principal_component DECIMAL(14,2) NOT NULL DEFAULT 0,
 interest_component DECIMAL(14,2) NOT NULL DEFAULT 0,
 notes VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_repayment_loan FOREIGN KEY(loan_id) REFERENCES loans(id),
 CONSTRAINT chk_repayment_received CHECK(received_amount > 0),
 CONSTRAINT chk_repayment_principal CHECK(principal_component >= 0),
 CONSTRAINT chk_repayment_interest CHECK(interest_component >= 0),
 CONSTRAINT chk_repayment_alloc CHECK(principal_component + interest_component <= received_amount),
 INDEX idx_repayment_loan(loan_id)
);
