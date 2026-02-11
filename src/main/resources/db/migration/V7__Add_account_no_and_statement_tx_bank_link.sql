ALTER TABLE account
    ADD COLUMN IF NOT EXISTS account_no BIGINT;

ALTER TABLE statement_transactions
    ADD COLUMN IF NOT EXISTS account_no BIGINT,
    ADD COLUMN IF NOT EXISTS approved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS bank_transaction_id UUID;

ALTER TABLE statement_transactions
    DROP CONSTRAINT IF EXISTS fk_statement_transaction_bank;

ALTER TABLE statement_transactions
    ADD CONSTRAINT fk_statement_transaction_bank
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transaction(id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_statement_transaction_bank_id
    ON statement_transactions(bank_transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_account_account_no
    ON account(account_no);
