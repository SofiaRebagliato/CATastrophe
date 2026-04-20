-- V1: Tablas del servicio social (posts, comments, likes, follows, messages)
-- Cada tabla usa UUIDs como clave primaria (generación distribuida entre microservicios)

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Posts (Meows) ──
CREATE TABLE posts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cat_id        UUID         NOT NULL,
    content       TEXT         NOT NULL,
    image_url     VARCHAR(500),
    post_type     VARCHAR(30)  NOT NULL DEFAULT 'meow',
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_cat_id ON posts(cat_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

COMMENT ON TABLE posts IS 'Publicaciones de los gatos — los "meows" de la plataforma';
COMMENT ON COLUMN posts.post_type IS 'meow, photo, adventure_share, challenge_result';

-- ── Comentarios ──
CREATE TABLE comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    cat_id     UUID      NOT NULL,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_cat_id ON comments(cat_id);

-- ── Likes ──
CREATE TABLE likes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    cat_id     UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Un gato solo puede dar like una vez por post
CREATE UNIQUE INDEX idx_likes_post_cat ON likes(post_id, cat_id);

-- ── Follows ──
CREATE TABLE follows (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID      NOT NULL,
    followed_id UUID      NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Un gato no puede seguir al mismo gato dos veces
CREATE UNIQUE INDEX idx_follows_unique ON follows(follower_id, followed_id);
CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_followed ON follows(followed_id);

-- ── Mensajes privados ──
CREATE TABLE messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id   UUID      NOT NULL,
    receiver_id UUID      NOT NULL,
    content     TEXT      NOT NULL,
    read        BOOLEAN   NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_receiver ON messages(receiver_id);
CREATE INDEX idx_messages_conversation ON messages(sender_id, receiver_id);
