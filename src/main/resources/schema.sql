-- This schema.sql file is used for LOCAL/DEV environments only
-- It is NOT managed by Flyway. Use this for quick local setup.
-- For production migrations, see: db/migration/V*.sql

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Documents table (existing)
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255),
    created TIMESTAMP,
    original_filename VARCHAR(255),
    file_path VARCHAR(255),
    document_type VARCHAR(255)
);

-- Corrections table (existing)
CREATE TABLE IF NOT EXISTS corrections (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(255),
    fields JSONB,
    note TEXT,
    correction_version INTEGER NOT NULL,
    saved_at TIMESTAMP NOT NULL,
    correction_placed_at TIMESTAMPTZ NOT NULL,
    saved_by VARCHAR(255),
    normalized_transactions_created INTEGER,
    CONSTRAINT fk_corrections_document FOREIGN KEY (document_id) REFERENCES documents(id)
);

-- Interpretation Jobs table (existing)
CREATE TABLE IF NOT EXISTS interpretation_jobs (
    id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error TEXT,
    document_type VARCHAR(255) NOT NULL
);

-- Interpretation Results table (existing)
CREATE TABLE IF NOT EXISTS interpretation_results (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    job_id VARCHAR(255) UNIQUE,
    document_type VARCHAR(255) NOT NULL,
    interpreted_at TIMESTAMP NOT NULL,
    extraction_methods VARCHAR(500),
    amount DOUBLE PRECISION,
    currency VARCHAR(255),
    date DATE,
    description TEXT,
    sender VARCHAR(255)
);

-- Statement Transactions table (existing)
CREATE TABLE IF NOT EXISTS statement_transactions (
    id BIGSERIAL PRIMARY KEY,
    interpretation_result_id BIGINT NOT NULL,
    amount DOUBLE PRECISION,
    currency VARCHAR(255),
    date DATE,
    description VARCHAR(1000),
    CONSTRAINT fk_statement_transaction_result FOREIGN KEY (interpretation_result_id) REFERENCES interpretation_results(id) ON DELETE CASCADE
);

-- Jobs table (existing)
CREATE TABLE IF NOT EXISTS jobs (
    id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(255),
    status VARCHAR(255),
    created TIMESTAMP,
    pipeline VARCHAR(255),
    use_ocr BOOLEAN,
    use_ai BOOLEAN,
    language_hint VARCHAR(255),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error TEXT
);

-- Account table
CREATE TABLE IF NOT EXISTS account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_account_name ON account(name);

-- Bank Transaction table
CREATE TABLE IF NOT EXISTS bank_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    booking_date DATE NOT NULL,
    value_date DATE NULL,
    amount NUMERIC(14,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    counterparty VARCHAR(200) NULL,
    description TEXT NOT NULL,
    reference VARCHAR(200) NULL,
    source_document_id UUID NULL,
    source_line_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_bank_transaction_account FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT uk_bank_transaction_account_hash UNIQUE (account_id, source_line_hash)
);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_account_id ON bank_transaction(account_id);
CREATE INDEX IF NOT EXISTS idx_bank_transaction_booking_date ON bank_transaction(booking_date);

-- Receipt table
CREATE TABLE IF NOT EXISTS receipt (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    purchase_date DATE NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    merchant VARCHAR(200) NULL,
    description TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_receipt_purchase_date ON receipt(purchase_date);
CREATE INDEX IF NOT EXISTS idx_receipt_created_at ON receipt(created_at);

-- Receipt Match table
CREATE TABLE IF NOT EXISTS receipt_match (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL,
    bank_transaction_id UUID NOT NULL,
    matched_amount NUMERIC(14,2) NOT NULL,
    match_type VARCHAR(20) NOT NULL,
    confidence NUMERIC(4,3) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_receipt_match_receipt FOREIGN KEY (receipt_id) REFERENCES receipt(id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_match_bank_transaction FOREIGN KEY (bank_transaction_id) REFERENCES bank_transaction(id) ON DELETE CASCADE,
    CONSTRAINT uk_receipt_match_receipt_transaction UNIQUE (receipt_id, bank_transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_receipt_match_receipt_id ON receipt_match(receipt_id);
CREATE INDEX IF NOT EXISTS idx_receipt_match_bank_transaction_id ON receipt_match(bank_transaction_id);
