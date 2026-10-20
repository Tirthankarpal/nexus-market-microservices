\c product_db;

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    price NUMERIC(19, 2),
    stock_quantity INTEGER
);

INSERT INTO products (name, price, stock_quantity) VALUES 
('Nexus Quantum Laptop', 1299.99, 5),
('Omni Pro Curved Monitor', 449.99, 12),
('Holographic Mech Keyboard', 189.50, 3),
('Chrono Cyber Watch', 299.00, 0),
('Apex Wireless Mouse', 89.99, 45),
('Void-ANC Sound Earbuds', 159.99, 8),
('Lumen RGB Smart Light Strips', 34.50, 15),
('Nexus Core SSD 2TB', 179.99, 2)
ON CONFLICT (name) DO NOTHING;
