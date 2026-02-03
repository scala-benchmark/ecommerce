
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255),
    customer_name VARCHAR(100),
    amount DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO transactions (id, description, customer_name, amount) VALUES
('TXN-001', 'Purchase of Electronics', 'John Smith', 299.99),
('TXN-002', 'Grocery Shopping', 'Jane Doe', 85.50),
('TXN-003', 'Online Subscription', 'Bob Johnson', 12.99),
('TXN-004', 'Book Purchase', 'Alice Williams', 45.00),
('TXN-005', 'Restaurant Payment', 'Charlie Brown', 67.25),
('TXN-006', 'Software License', 'David Miller', 199.00),
('TXN-007', 'Travel Booking', 'Eva Garcia', 450.00),
('TXN-008', 'Clothing Purchase', 'Frank Wilson', 120.75),
('TXN-009', 'Home Appliances', 'Grace Lee', 350.00),
('TXN-010', 'Office Supplies', 'Henry Taylor', 89.99);
