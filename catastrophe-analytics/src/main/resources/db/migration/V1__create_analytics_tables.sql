-- V1: Tablas del servicio de análisis
-- Personalidades felinas calculadas a partir de los eventos del bus.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Personalidades ──
-- Cada gato tiene un score por trait. La tabla mantiene una fila por cada
-- combinación (cat_id, trait) y se va actualizando con exponential smoothing
-- conforme llegan eventos al consumer.
--
-- Decisión de diseño: usamos índice único compuesto (cat_id, trait) para
-- que cada trait se actualice in-place (UPDATE) en lugar de duplicarse.
-- Es lo que pide la spec en "Decisiones de diseño destacables — Upsert".
CREATE TABLE cat_personalities (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cat_id      UUID         NOT NULL,
    trait       VARCHAR(30)  NOT NULL,
    score       DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_cat_personalities_cat_trait
    ON cat_personalities(cat_id, trait);

CREATE INDEX idx_cat_personalities_cat
    ON cat_personalities(cat_id);

COMMENT ON TABLE cat_personalities IS 'Traits de personalidad por gato — exponential smoothing';
COMMENT ON COLUMN cat_personalities.trait IS 'PLAYFUL, LAZY, HUNTER, SOCIAL, MYSTERIOUS';
COMMENT ON COLUMN cat_personalities.score IS 'Score normalizado [0.0, 1.0]';

-- ── Idempotencia del consumer ──
-- Mismo patrón que en notifications y profiles: registrar el event_id de
-- cada evento procesado para no aplicar dos veces el mismo impulso al trait.
CREATE TABLE processed_personality_events (
    event_id      UUID      PRIMARY KEY,
    cat_id        UUID      NOT NULL,
    event_type    VARCHAR(50) NOT NULL,
    processed_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_personality_events_cat
    ON processed_personality_events(cat_id);

COMMENT ON TABLE processed_personality_events IS 'Idempotencia del consumer de personalidades';
