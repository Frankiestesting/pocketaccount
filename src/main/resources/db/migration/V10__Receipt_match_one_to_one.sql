CREATE UNIQUE INDEX IF NOT EXISTS idx_receipt_match_receipt_id_unique
    ON receipt_match(receipt_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_receipt_match_bank_transaction_id_unique
    ON receipt_match(bank_transaction_id);
