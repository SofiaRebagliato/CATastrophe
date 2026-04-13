-- V1: Tabla de humanos (los "asistentes" de los gatos)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE humans (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username       VARCHAR(50)  UNIQUE NOT NULL,
    email          VARCHAR(255) UNIQUE NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(100),
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    last_login     TIMESTAMP,
    active         BOOLEAN      NOT NULL DEFAULT true
);

COMMENT ON TABLE humans IS 'Los humanos son meros "asistentes" de sus gatos';
