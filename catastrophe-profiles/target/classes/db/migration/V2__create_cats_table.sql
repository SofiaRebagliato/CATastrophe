-- V2: Tabla de gatos (los verdaderos usuarios de la plataforma)
CREATE TABLE cats (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    human_id    UUID         NOT NULL REFERENCES humans(id),
    name        VARCHAR(100) NOT NULL,
    breed       VARCHAR(100),
    age_months  INT,
    avatar_url  VARCHAR(500),
    bio         TEXT,
    xp          INT          NOT NULL DEFAULT 0,
    level       INT          NOT NULL DEFAULT 1,
    mood        VARCHAR(50)  DEFAULT 'curious',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP
);

-- Un humano no puede tener dos gatos con el mismo nombre
CREATE UNIQUE INDEX idx_cats_human_name ON cats(human_id, name);

-- Índice para buscar gatos de un humano
CREATE INDEX idx_cats_human_id ON cats(human_id);

COMMENT ON TABLE cats IS 'Los gatos son los verdaderos usuarios de la plataforma';
COMMENT ON COLUMN cats.avatar_url IS 'Imagen de TheCatAPI';
