CREATE TABLE users
(
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'OPERATOR',
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    company_id BIGINT       NOT NULL REFERENCES companies (id),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_company_id ON users (company_id);
CREATE INDEX idx_users_email ON users (email);
