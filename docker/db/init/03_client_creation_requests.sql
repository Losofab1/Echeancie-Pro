CREATE TABLE client_creation_requests (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reviewed_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT client_creation_requests_review_ck CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL) OR
        (status <> 'PENDING' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX client_creation_requests_pending_email_uq
    ON client_creation_requests (LOWER(email))
    WHERE status = 'PENDING';

CREATE INDEX client_creation_requests_created_at_idx
    ON client_creation_requests (created_at DESC);