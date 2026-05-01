-- V1: Tabla de notificaciones
-- Una notificación se materializa por cada evento "interesante" del bus Kafka
-- y se entrega al gato destinatario vía REST.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notifications (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id           UUID         NOT NULL UNIQUE,
    recipient_cat_id   UUID         NOT NULL,
    type               VARCHAR(40)  NOT NULL,
    message            VARCHAR(500) NOT NULL,
    payload            JSONB        NOT NULL DEFAULT '{}'::jsonb,
    read               BOOLEAN      NOT NULL DEFAULT false,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    read_at            TIMESTAMP
);

-- Índice principal: feed paginado del gato (no leídas primero, luego por fecha desc)
CREATE INDEX idx_notifications_recipient_created
    ON notifications(recipient_cat_id, created_at DESC);

-- Índice parcial: contador de no-leídas, muy frecuente en el frontend
CREATE INDEX idx_notifications_unread
    ON notifications(recipient_cat_id)
    WHERE read = false;

COMMENT ON TABLE notifications IS 'Notificaciones materializadas a partir de eventos Kafka';
COMMENT ON COLUMN notifications.event_id IS 'ID del CatastropheEvent original — garantiza idempotencia';
COMMENT ON COLUMN notifications.type IS 'POST_LIKED, POST_COMMENTED, CAT_FOLLOWED, ADVENTURE_COMPLETED, CHALLENGE_COMPLETED, BADGE_EARNED, LEVEL_UP';
COMMENT ON COLUMN notifications.payload IS 'Datos contextuales del evento (postId, badgeName, xpEarned, etc.)';

-- ── Tracker de nivel por gato ──
-- Necesario para detectar level-ups reales: el evento XpGained trae el nuevo
-- nivel pero no el previo, así que mantenemos aquí el último conocido.
CREATE TABLE cat_level_state (
    cat_id      UUID      PRIMARY KEY,
    last_level  INT       NOT NULL DEFAULT 1,
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

COMMENT ON TABLE cat_level_state IS 'Último nivel conocido por gato — usado solo para detectar level-ups';
