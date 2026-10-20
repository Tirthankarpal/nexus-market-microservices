\c inventory_db;

CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    sku_code VARCHAR(255) NOT NULL UNIQUE,
    quantity INTEGER NOT NULL
);

INSERT INTO inventory (sku_code, quantity) VALUES 
('Nexus Quantum Laptop', 5),
('Omni Pro Curved Monitor', 12),
('Holographic Mech Keyboard', 3),
('Chrono Cyber Watch', 0),
('Apex Wireless Mouse', 45),
('Void-ANC Sound Earbuds', 8),
('Lumen RGB Smart Light Strips', 15),
('Nexus Core SSD 2TB', 2)
ON CONFLICT (sku_code) DO NOTHING;
