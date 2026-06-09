CREATE TABLE IF NOT EXISTS tickets (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    user_id UUID NOT NULL,
    game_id UUID NOT NULL,
    totp_secret VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    -- Status: ACTIVE | USED | REVOKED
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_tickets_user_id ON tickets(user_id);