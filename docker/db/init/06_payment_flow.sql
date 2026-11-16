-- Payment-flow schema used by the JSF front-end (credit balance, payment groups,
-- allocated amounts and stored proofs). Written idempotently so it can be applied
-- whether or not the statements were already executed manually.

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS credit_balance BIGINT NOT NULL DEFAULT 0;

ALTER TABLE payment_submissions
    ADD COLUMN IF NOT EXISTS payment_group_id UUID,
    ADD COLUMN IF NOT EXISTS payment_group_amount BIGINT,
    ADD COLUMN IF NOT EXISTS credit_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_date DATE,
    ADD COLUMN IF NOT EXISTS proof_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS proof_content_type VARCHAR(128),
    ADD COLUMN IF NOT EXISTS proof_data BYTEA;

CREATE INDEX IF NOT EXISTS idx_payment_submissions_group_id
    ON payment_submissions (payment_group_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_payment_submissions_credit_nonnegative'
    ) THEN
        ALTER TABLE payment_submissions
            ADD CONSTRAINT chk_payment_submissions_credit_nonnegative
            CHECK (credit_amount >= 0);
    END IF;
END
$$;
