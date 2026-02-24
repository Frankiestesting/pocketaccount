ALTER TABLE receipt_match
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE receipt_match
    SET status = 'ACTIVE'
    WHERE status IS NULL;

ALTER TABLE receipt_match
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE receipt_match
    DROP CONSTRAINT IF EXISTS uk_receipt_match_receipt_transaction;

DROP INDEX IF EXISTS idx_receipt_match_receipt_id_unique;
DROP INDEX IF EXISTS idx_receipt_match_bank_transaction_id_unique;

CREATE UNIQUE INDEX IF NOT EXISTS idx_receipt_match_receipt_id_active
    ON receipt_match(receipt_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_receipt_match_bank_transaction_id_active
    ON receipt_match(bank_transaction_id)
    WHERE status = 'ACTIVE';
