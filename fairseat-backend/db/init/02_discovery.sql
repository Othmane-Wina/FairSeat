CREATE TABLE IF NOT EXISTS venues (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    capacity INT NOT NULL
    );

CREATE TABLE IF NOT EXISTS games (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    venue_id UUID REFERENCES venues(id),
    event_date TIMESTAMP NOT NULL,
    description TEXT,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Données de démo
INSERT INTO venues (name, city, capacity) VALUES
                                              ('Stade Mohammed V', 'Casablanca', 67000),
                                              ('Complexe Sportif de Fès', 'Fès', 45000);

INSERT INTO games (name, venue_id, event_date, description, total_seats, available_seats)
SELECT
    'Wydad AC vs Raja CA',
    v.id,
    NOW() + INTERVAL '7 days',
    'Derby de Casablanca - Phase de groupes',
    67000,
    67000
FROM venues v WHERE v.name = 'Stade Mohammed V';