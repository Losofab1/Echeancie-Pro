ALTER TABLE clients
    ADD COLUMN credit_balance BIGINT NOT NULL DEFAULT 0;

ALTER TABLE payment_submissions
    ADD COLUMN payment_group_id UUID,
    ADD COLUMN payment_group_amount BIGINT,
    ADD COLUMN credit_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN payment_date DATE,
    ADD COLUMN proof_file_name VARCHAR(255),
    ADD COLUMN proof_content_type VARCHAR(128),
    ADD COLUMN proof_data BYTEA;

CREATE INDEX idx_payment_submissions_group_id
    ON payment_submissions (payment_group_id);

ALTER TABLE payment_submissions
    ADD CONSTRAINT chk_payment_submissions_credit_nonnegative
    CHECK (credit_amount >= 0);
