ALTER TABLE clients
    ADD COLUMN email_reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE email_outbox (
    id UUID PRIMARY KEY,
    event_key VARCHAR(200) NOT NULL UNIQUE,
    recipient VARCHAR(254) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(1000)
);

CREATE INDEX email_outbox_pending_idx
    ON email_outbox (next_attempt_at, created_at)
    WHERE status = 'PENDING';
