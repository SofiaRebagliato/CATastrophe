-- V1: Tablas del servicio de aventuras y gamificación
-- Adventures, challenges, badges, rankings, XP/niveles

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Aventuras ──
CREATE TABLE adventures (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    difficulty      VARCHAR(20)  NOT NULL,
    xp_reward       INT          NOT NULL,
    adventure_type  VARCHAR(50)  NOT NULL,
    repeatable      BOOLEAN      NOT NULL DEFAULT false,
    available_from  TIMESTAMP,
    available_until TIMESTAMP
);

CREATE INDEX idx_adventures_type ON adventures(adventure_type);
CREATE INDEX idx_adventures_available ON adventures(available_from);

COMMENT ON TABLE adventures IS 'Misiones disponibles para los gatos aventureros';
COMMENT ON COLUMN adventures.difficulty IS 'easy, medium, hard, legendary';
COMMENT ON COLUMN adventures.adventure_type IS 'daily, weekly, special';

-- ── Aventuras de gatos (relación N:M con estado) ──
CREATE TABLE cat_adventures (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cat_id       UUID         NOT NULL,
    adventure_id UUID         NOT NULL REFERENCES adventures(id),
    status       VARCHAR(20)  NOT NULL DEFAULT 'in_progress',
    progress_pct INT          NOT NULL DEFAULT 0,
    started_at   TIMESTAMP    NOT NULL DEFAULT now(),
    completed_at TIMESTAMP
);

CREATE INDEX idx_cat_adventures_cat ON cat_adventures(cat_id);
CREATE INDEX idx_cat_adventures_cat_adv ON cat_adventures(cat_id, adventure_id);

COMMENT ON COLUMN cat_adventures.status IS 'in_progress, completed, failed, abandoned';

-- ── Retos PvP ──
CREATE TABLE challenges (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    challenge_type VARCHAR(50)  NOT NULL,
    xp_reward      INT          NOT NULL,
    starts_at      TIMESTAMP,
    ends_at        TIMESTAMP
);

CREATE INDEX idx_challenges_type ON challenges(challenge_type);

COMMENT ON COLUMN challenges.challenge_type IS 'hunting, racing, napping, grooming';

-- ── Participación en retos ──
CREATE TABLE cat_challenges (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cat_id       UUID         NOT NULL,
    challenge_id UUID         NOT NULL REFERENCES challenges(id),
    opponent_id  UUID,
    status       VARCHAR(20)  NOT NULL DEFAULT 'pending',
    score        INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_cat_challenges_cat ON cat_challenges(cat_id);
CREATE INDEX idx_cat_challenges_challenge ON cat_challenges(challenge_id);
CREATE INDEX idx_cat_challenges_opponent ON cat_challenges(opponent_id);

COMMENT ON COLUMN cat_challenges.opponent_id IS 'Null si es un reto abierto esperando rival';
COMMENT ON COLUMN cat_challenges.status IS 'pending, active, won, lost, draw';

-- ── Badges / Logros ──
CREATE TABLE badges (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    icon_url    VARCHAR(500),
    rarity      VARCHAR(20) NOT NULL
);

CREATE INDEX idx_badges_rarity ON badges(rarity);

COMMENT ON COLUMN badges.rarity IS 'common, rare, epic, legendary';

-- ── Badges ganados por gatos ──
CREATE TABLE cat_badges (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cat_id    UUID      NOT NULL,
    badge_id  UUID      NOT NULL REFERENCES badges(id),
    earned_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_cat_badges_unique ON cat_badges(cat_id, badge_id);

-- ── Recompensas de aventuras (badges asociados) ──
CREATE TABLE adventure_rewards (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adventure_id UUID NOT NULL REFERENCES adventures(id),
    badge_id     UUID NOT NULL REFERENCES badges(id)
);

CREATE UNIQUE INDEX idx_adventure_rewards_unique ON adventure_rewards(adventure_id, badge_id);

-- ── Datos iniciales: aventuras y badges por defecto ──
INSERT INTO badges (id, name, description, icon_url, rarity) VALUES
    ('a0000001-0000-0000-0000-000000000001', 'Primer Meow', 'Publicaste tu primer meow', '/badges/primer-meow.png', 'common'),
    ('a0000001-0000-0000-0000-000000000002', 'Cazador Novato', 'Completaste tu primera aventura de caza', '/badges/cazador-novato.png', 'common'),
    ('a0000001-0000-0000-0000-000000000003', 'Explorador', 'Completaste 5 aventuras', '/badges/explorador.png', 'rare'),
    ('a0000001-0000-0000-0000-000000000004', 'Campeón PvP', 'Ganaste 10 retos', '/badges/campeon-pvp.png', 'epic'),
    ('a0000001-0000-0000-0000-000000000005', 'Leyenda Felina', 'Alcanzaste nivel 20', '/badges/leyenda-felina.png', 'legendary'),
    ('a0000001-0000-0000-0000-000000000006', 'Rey de la Siesta', 'Ganaste 5 retos de siesta', '/badges/rey-siesta.png', 'rare'),
    ('a0000001-0000-0000-0000-000000000007', 'Velocista', 'Ganaste 5 retos de carrera', '/badges/velocista.png', 'rare'),
    ('a0000001-0000-0000-0000-000000000008', 'Social Butterfly', 'Tienes más de 50 seguidores', '/badges/social-butterfly.png', 'epic');

INSERT INTO adventures (id, title, description, difficulty, xp_reward, adventure_type, repeatable) VALUES
    ('b0000001-0000-0000-0000-000000000001', 'La Caja Misteriosa', 'Investiga esa caja de cartón que acaba de llegar', 'easy', 50, 'daily', true),
    ('b0000001-0000-0000-0000-000000000002', 'Caza del Punto Rojo', 'Persigue el misterioso punto rojo por toda la casa', 'easy', 75, 'daily', true),
    ('b0000001-0000-0000-0000-000000000003', 'Escalada al Armario', 'Conquista la cima del armario más alto', 'medium', 150, 'weekly', true),
    ('b0000001-0000-0000-0000-000000000004', 'El Gran Escape', 'Planea y ejecuta una fuga al jardín', 'hard', 300, 'weekly', false),
    ('b0000001-0000-0000-0000-000000000005', 'Vigía Nocturno', 'Patrulla toda la casa durante la noche sin ser detectado', 'hard', 350, 'weekly', true),
    ('b0000001-0000-0000-0000-000000000006', 'La Leyenda del Atún', 'Completa la búsqueda legendaria del atún dorado', 'legendary', 1000, 'special', false);

INSERT INTO challenges (id, title, description, challenge_type, xp_reward) VALUES
    ('c0000001-0000-0000-0000-000000000001', 'Duelo de Caza', 'Demuestra quién es el mejor cazador', 'hunting', 100),
    ('c0000001-0000-0000-0000-000000000002', 'Carrera Felina', 'Primer gato en dar 3 vueltas al salón', 'racing', 100),
    ('c0000001-0000-0000-0000-000000000003', 'Siesta Suprema', 'El gato que duerma más tiempo gana', 'napping', 80),
    ('c0000001-0000-0000-0000-000000000004', 'Concurso de Acicalamiento', 'El gato más limpio y elegante gana', 'grooming', 80);

INSERT INTO adventure_rewards (adventure_id, badge_id) VALUES
    ('b0000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000002'),
    ('b0000001-0000-0000-0000-000000000006', 'a0000001-0000-0000-0000-000000000005');
