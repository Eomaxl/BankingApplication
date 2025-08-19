-- Performance and Consistency Indexes for Banking System

-- Account table indexes for fast lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_account_number ON accounts(account_number);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_account_holder_id ON accounts(account_holder_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_bank_id ON accounts(bank_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_status ON accounts(account_status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_balance ON accounts(balance);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_created_at ON accounts(created_at);

-- Transaction table indexes for fast transaction history queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_transaction_id ON transactions(transaction_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_transaction_type ON transactions(transaction_type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_status ON transactions(transaction_status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_amount ON transactions(amount);

-- Composite indexes for common query patterns
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_account_date ON transactions(account_id, transaction_date DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_account_type ON transactions(account_id, transaction_type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_holder_status ON accounts(account_holder_id, account_status);

-- Person table indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_persons_email ON persons(email);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_persons_phone ON persons(phone_number);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_persons_name ON persons(first_name, last_name);

-- Account holder indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_account_holders_customer_id ON account_holders(customer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_account_holders_person_id ON account_holders(person_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_account_holders_status ON account_holders(account_holder_status);

-- Bank table indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_banks_bank_code ON banks(bank_code);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_banks_name ON banks(bank_name);

-- Partial indexes for active records (most common queries)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_active ON accounts(account_number, balance)
    WHERE account_status = 'ACTIVE';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_completed ON transactions(account_id, transaction_date DESC)
    WHERE transaction_status = 'COMPLETED';

-- Check constraints for data integrity
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_balance_non_negative
    CHECK (balance >= 0);

ALTER TABLE transactions ADD CONSTRAINT chk_transactions_amount_positive
    CHECK (amount > 0);

-- Unique constraints for business rules
ALTER TABLE accounts ADD CONSTRAINT uk_accounts_account_number UNIQUE (account_number);
ALTER TABLE persons ADD CONSTRAINT uk_persons_email UNIQUE (email);
ALTER TABLE account_holders ADD CONSTRAINT uk_account_holders_customer_id UNIQUE (customer_id);
ALTER TABLE banks ADD CONSTRAINT uk_banks_bank_code UNIQUE (bank_code);

-- Foreign key indexes (PostgreSQL doesn't auto-create these)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_fk_bank ON accounts(bank_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_fk_account_holder ON accounts(account_holder_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_fk_account ON transactions(account_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_fk_target_account ON transactions(target_account_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_account_holders_fk_person ON account_holders(person_id);

-- Statistics update for query planner optimization
ANALYZE accounts;
ANALYZE transactions;
ANALYZE persons;
ANALYZE account_holders;
ANALYZE banks;