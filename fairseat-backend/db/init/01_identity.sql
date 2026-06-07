-- Table des utilisateurs principaux
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL UNIQUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    device_fingerprint VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Table de liaison entre un user et son appareil physique
CREATE TABLE IF NOT EXISTS device_bindings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hardware_key VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Index pour accélerer les recherches par téléphone
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);

-- Données de test pour la soutenance
INSERT INTO users (phone, is_verified) VALUES
                                           ('+212600000001', false),
                                           ('+212600000002', false)
    ON CONFLICT DO NOTHING;