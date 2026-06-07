CREATE TABLE IF NOT EXISTS resale_listings (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    original_price DECIMAL(10,2) NOT NULL,
    resale_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );