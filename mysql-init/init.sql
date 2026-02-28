CREATE TABLE IF NOT EXISTS category (
    categoryid VARCHAR(36) PRIMARY KEY,
    categoryname VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS manufacturer (
    manufacturerid VARCHAR(36) PRIMARY KEY,
    manufacturername VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    productid VARCHAR(36) PRIMARY KEY,
    categoryid VARCHAR(36),
    manufacturerid VARCHAR(36),
    productcode VARCHAR(50) NOT NULL,
    displayname VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE NOT NULL,
    FOREIGN KEY (categoryid) REFERENCES category(categoryid),
    FOREIGN KEY (manufacturerid) REFERENCES manufacturer(manufacturerid)
);

INSERT INTO category (categoryid, categoryname) VALUES
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Electronics'),
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Books'),
('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Clothing'),
('d4e5f6a7-b8c9-0123-defa-234567890123', 'Home & Garden');

INSERT INTO manufacturer (manufacturerid, manufacturername) VALUES
('11111111-1111-1111-1111-111111111111', 'TechCorp'),
('22222222-2222-2222-2222-222222222222', 'BookWorld'),
('33333333-3333-3333-3333-333333333333', 'FashionLine'),
('44444444-4444-4444-4444-444444444444', 'HomeStyle');

INSERT INTO product (productid, categoryid, manufacturerid, productcode, displayname, description, price) VALUES
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0001', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11111111-1111-1111-1111-111111111111', 'ELEC-001', 'Wireless Headphones', 'Premium noise-cancelling wireless headphones', 149.99),
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0002', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11111111-1111-1111-1111-111111111111', 'ELEC-002', 'Smart Watch Pro', 'Advanced fitness and health tracking smartwatch', 299.99),
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0003', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', '22222222-2222-2222-2222-222222222222', 'BOOK-001', 'Scala Programming Guide', 'Comprehensive guide to Scala programming', 45.00),
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0004', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', '22222222-2222-2222-2222-222222222222', 'BOOK-002', 'Data Structures in Java', 'Essential data structures and algorithms', 55.00),
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0005', 'c3d4e5f6-a7b8-9012-cdef-123456789012', '33333333-3333-3333-3333-333333333333', 'CLTH-001', 'Cotton T-Shirt', 'Premium cotton t-shirt in various colors', 29.99),
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0006', 'd4e5f6a7-b8c9-0123-defa-234567890123', '44444444-4444-4444-4444-444444444444', 'HOME-001', 'Garden Tool Set', 'Complete garden tool set with carrying case', 89.50),
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0007', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', '11111111-1111-1111-1111-111111111111', 'ELEC-003', 'Bluetooth Speaker', 'Portable waterproof bluetooth speaker', 79.99),
('aaaa1111-bbbb-cccc-dddd-eeeeeeee0008', 'c3d4e5f6-a7b8-9012-cdef-123456789012', '33333333-3333-3333-3333-333333333333', 'CLTH-002', 'Denim Jacket', 'Classic denim jacket with modern fit', 120.00);
