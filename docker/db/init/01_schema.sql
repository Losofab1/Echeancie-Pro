CREATE TABLE clients (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(32),
    sms_reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX clients_email_lower_uq ON clients (LOWER(email));

CREATE TABLE users (
    id UUID PRIMARY KEY,
    client_id UUID REFERENCES clients(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'CLIENT')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_client_role_ck CHECK (
        (role = 'CLIENT' AND client_id IS NOT NULL) OR
        (role <> 'CLIENT' AND client_id IS NULL)
    )
);
CREATE UNIQUE INDEX users_email_lower_uq ON users (LOWER(email));

CREATE TABLE payment_requests (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total NUMERIC(14, 0) NOT NULL CHECK (total > 0),
    installment_count SMALLINT NOT NULL CHECK (installment_count BETWEEN 1 AND 5),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CONFIRMED')),
    note VARCHAR(300),
    channel VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    reviewed_at TIMESTAMPTZ,
    agreed_total NUMERIC(14, 0) CHECK (agreed_total IS NULL OR agreed_total > 0),
    schedule_id UUID UNIQUE
);

CREATE TABLE payment_request_installments (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES payment_requests(id) ON DELETE CASCADE,
    installment_number SMALLINT NOT NULL CHECK (installment_number BETWEEN 1 AND 5),
    due_date DATE NOT NULL,
    CONSTRAINT payment_request_installments_number_uq UNIQUE (request_id, installment_number),
    CONSTRAINT payment_request_installments_date_uq UNIQUE (request_id, due_date)
);

CREATE TABLE payment_schedules (
    id UUID PRIMARY KEY,
    reference VARCHAR(32) NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    manager_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total NUMERIC(14, 0) NOT NULL CHECK (total > 0),
    confirmation_status VARCHAR(24) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (confirmation_status IN ('AWAITING_CLIENT', 'CONFIRMED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE payment_requests
    ADD CONSTRAINT payment_requests_schedule_fk
    FOREIGN KEY (schedule_id) REFERENCES payment_schedules(id) ON DELETE RESTRICT;

CREATE TABLE installments (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES payment_schedules(id) ON DELETE CASCADE,
    installment_number SMALLINT NOT NULL CHECK (installment_number BETWEEN 1 AND 5),
    amount NUMERIC(14, 0) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    paid_at TIMESTAMPTZ,
    channel VARCHAR(32),
    payment_reference VARCHAR(80),
    CONSTRAINT installments_number_uq UNIQUE (schedule_id, installment_number)
);
CREATE INDEX installments_due_date_idx ON installments (due_date) WHERE paid = FALSE;

CREATE TABLE payment_submissions (
    id UUID PRIMARY KEY,
    installment_id UUID NOT NULL REFERENCES installments(id) ON DELETE RESTRICT,
    submitted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    amount NUMERIC(14, 0) NOT NULL CHECK (amount > 0),
    channel VARCHAR(32) NOT NULL,
    reference VARCHAR(80),
    note VARCHAR(240),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reason VARCHAR(240),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    reviewed_at TIMESTAMPTZ
);
CREATE INDEX payment_submissions_installment_idx ON payment_submissions (installment_id, submitted_at DESC);
