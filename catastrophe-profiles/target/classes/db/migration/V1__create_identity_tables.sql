-- V1: Tablas del servicio de perfiles e identidad (humans, cats)
-- Patrón "database-per-service": esta migración solo crea las tablas
-- propias del dominio de perfiles. El resto de servicios tienen sus
-- propias bases de datos y migraciones independientes.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Humans ──
-- Cuentas de los humanos que actúan como "asistentes" de sus gatos.
-- Usadas para autenticación con Spring Security.
CREATE TABLE humans (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(100),
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    last_login     TIMESTAMP,
    active         BOOLEAN      NOT NULL DEFAULT true
);

-- Índices secundarios para los lookups más frecuentes (login y búsqueda por email)
CREATE INDEX idx_humans_username ON humans(username);
CREATE INDEX idx_humans_email    ON humans(email);

COMMENT ON TABLE humans IS 'Cuentas de humanos — los "asistentes" de los gatos';
COMMENT ON COLUMN humans.password_hash IS 'BCrypt hash gestionado por Spring Security';
COMMENT ON COLUMN humans.active IS 'Cuenta deshabilitada lógicamente (soft-delete)';

-- ── Cats ──
-- Perfiles de gatos. Un humano puede gestionar varios gatos (relación 1:N).
-- Aquí viven los contadores de XP/nivel: cada acción gamificada del gato
-- (completar aventura, ganar reto) actualiza estos valores vía consumer Kafka.
CREATE TABLE cats (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    human_id    UUID         NOT NULL REFERENCES humans(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    breed       VARCHAR(100),
    age_months  INT,
    avatar_url  VARCHAR(500),
    bio         TEXT,
    xp          INT          NOT NULL DEFAULT 0,
    level       INT          NOT NULL DEFAULT 1,
    mood        VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP
);

-- Índice para listar los gatos de un humano (acceso muy frecuente desde el dashboard)
CREATE INDEX idx_cats_human_id ON cats(human_id);

-- Índice para rankings y consultas top-N por XP
CREATE INDEX idx_cats_xp ON cats(xp DESC);

COMMENT ON TABLE cats IS 'Perfiles de gatos — los verdaderos usuarios de CATastrophe';
COMMENT ON COLUMN cats.xp IS 'XP total acumulado del gato (curva cuadrática: nivel N requiere N²·100 XP)';
COMMENT ON COLUMN cats.mood IS 'Estado de ánimo derivado del clima u otras señales (CatMood)';

-- ── Tracker de eventos XP ya procesados ──
-- Garantiza idempotencia del consumer Kafka que aplica XP a los gatos:
-- si un evento de gamificación llega dos veces (reintento, rebalanceo),
-- la segunda aplicación no duplica el XP.
CREATE TABLE processed_xp_events (
    event_id      UUID      PRIMARY KEY,
    cat_id        UUID      NOT NULL,
    amount        INT       NOT NULL,
    source        VARCHAR(50) NOT NULL,
    processed_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_xp_events_cat ON processed_xp_events(cat_id);

COMMENT ON TABLE processed_xp_events IS 'Idempotencia del consumer de XP — un evento procesado nunca aplica XP dos veces';
