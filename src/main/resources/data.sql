-- Sample data for testing
-- Banks
INSERT INTO banks (bank_name, bank_code, address, phone_number, email, created_at, updated_at) VALUES
                                                                                                   ('First National Bank', 'FNB001', '123 Main Street, New York, NY 10001', '+1-555-0101', 'info@fnb.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                   ('Global Trust Bank', 'GTB002', '456 Wall Street, New York, NY 10005', '+1-555-0102', 'contact@gtb.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                   ('Community Savings Bank', 'CSB003', '789 Broadway, New York, NY 10003', '+1-555-0103', 'support@csb.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Persons
INSERT INTO persons (first_name, last_name, email, phone_number, address, date_of_birth, created_at, updated_at) VALUES
                                                                                                                     ('John', 'Doe', 'john.doe@email.com', '+1-555-1001', '100 Oak Street, New York, NY 10001', '1985-05-15 00:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                     ('Jane', 'Smith', 'jane.smith@email.com', '+1-555-1002', '200 Pine Street, New York, NY 10002', '1990-08-22 00:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                     ('Bob', 'Johnson', 'bob.johnson@email.com', '+1-555-1003', '300 Elm Street, New York, NY 10003', '1988-12-10 00:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                     ('Alice', 'Williams', 'alice.williams@email.com', '+1-555-1004', '400 Maple Street, New York, NY 10004', '1992-03-18 00:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Account Holders
INSERT INTO account_holders (person_id, customer_id, account_holder_status, created_at, updated_at) VALUES
                                                                                                        (1, 'CUST001', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        (2, 'CUST002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        (3, 'CUST003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        (4, 'CUST004', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Accounts
INSERT INTO accounts (account_number, balance, account_type, account_status, bank_id, account_holder_id, created_at, updated_at) VALUES
                                                                                                                                     ('ACC001001', 5000.00, 'SAVINGS', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                     ('ACC001002', 2500.50, 'CHECKING', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                     ('ACC002001', 10000.00, 'SAVINGS', 'ACTIVE', 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                     ('ACC002002', 1500.75, 'CHECKING', 'ACTIVE', 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                     ('ACC003001', 7500.25, 'BUSINESS', 'ACTIVE', 3, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                     ('ACC001003', 25000.00, 'INVESTMENT', 'ACTIVE', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample Transactions
INSERT INTO transactions (transaction_id, amount, transaction_type, transaction_status, description, account_id, balance_before, balance_after, transaction_date, created_at) VALUES
                                                                                                                                                                                  ('TXN001', 1000.00, 'DEPOSIT', 'COMPLETED', 'Initial deposit', 1, 4000.00, 5000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                  ('TXN002', 500.00, 'WITHDRAWAL', 'COMPLETED', 'ATM withdrawal', 1, 5000.00, 4500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                  ('TXN003', 2000.00, 'DEPOSIT', 'COMPLETED', 'Salary deposit', 2, 500.50, 2500.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                  ('TXN004', 5000.00, 'DEPOSIT', 'COMPLETED', 'Initial deposit', 3, 5000.00, 10000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                  ('TXN005', 1000.00, 'DEPOSIT', 'COMPLETED', 'Business income', 5, 6500.25, 7500.25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);