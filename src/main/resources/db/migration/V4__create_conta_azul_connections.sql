CREATE TABLE conta_azul_connections (
    id                    BIGSERIAL     PRIMARY KEY,
    company_id            BIGINT        NOT NULL UNIQUE REFERENCES companies(id),
    access_token          VARCHAR(2000) NOT NULL,
    refresh_token         VARCHAR(2000) NOT NULL,
    expires_at            TIMESTAMPTZ   NOT NULL,
    status                VARCHAR(30)   NOT NULL,
    external_company_name VARCHAR(255),
    external_company_id   VARCHAR(100),
    last_sync_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ca_conn_company_id ON conta_azul_connections(company_id);
CREATE INDEX idx_ca_conn_status ON conta_azul_connections(status);