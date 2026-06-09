CREATE TABLE IF NOT EXISTS bookings (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    game_id UUID NOT NULL,
    seat_id VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING | CONFIRMED | CANCELLED | EXPIRED
    admission_token VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL DEFAULT NOW() + INTERVAL '10 minutes'
    );

CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);