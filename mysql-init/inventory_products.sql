CREATE TABLE IF NOT EXISTS inventory_products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    product_type VARCHAR(100) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    warehouse_location VARCHAR(100)
);

INSERT INTO inventory_products (product_name, product_type, sku, unit_price, stock_quantity, warehouse_location) VALUES
('Wireless Mouse', 'peripherals', 'PER-001', 29.99, 150, 'Warehouse A - Shelf 3'),
('Mechanical Keyboard', 'peripherals', 'PER-002', 89.99, 75, 'Warehouse A - Shelf 3'),
('USB-C Hub', 'peripherals', 'PER-003', 45.00, 200, 'Warehouse A - Shelf 4'),
('27" LED Monitor', 'monitors', 'MON-001', 349.99, 40, 'Warehouse B - Shelf 1'),
('34" Ultrawide Monitor', 'monitors', 'MON-002', 599.99, 25, 'Warehouse B - Shelf 1'),
('Laptop Stand', 'accessories', 'ACC-001', 39.99, 300, 'Warehouse A - Shelf 7'),
('Webcam HD 1080p', 'peripherals', 'PER-004', 59.99, 120, 'Warehouse A - Shelf 5'),
('Noise Cancelling Headset', 'audio', 'AUD-001', 129.99, 85, 'Warehouse C - Shelf 2'),
('Desktop Speakers', 'audio', 'AUD-002', 74.99, 60, 'Warehouse C - Shelf 2'),
('Portable SSD 1TB', 'storage', 'STR-001', 109.99, 180, 'Warehouse A - Shelf 9'),
('External HDD 2TB', 'storage', 'STR-002', 79.99, 95, 'Warehouse A - Shelf 9'),
('Docking Station', 'accessories', 'ACC-002', 159.99, 50, 'Warehouse B - Shelf 4'),
('Graphics Tablet', 'peripherals', 'PER-005', 199.99, 35, 'Warehouse B - Shelf 6'),
('Ergonomic Chair Mat', 'accessories', 'ACC-003', 49.99, 110, 'Warehouse C - Shelf 8'),
('USB Microphone', 'audio', 'AUD-003', 99.99, 70, 'Warehouse C - Shelf 3');
