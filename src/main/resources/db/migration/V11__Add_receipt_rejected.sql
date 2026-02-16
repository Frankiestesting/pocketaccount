ALTER TABLE receipt
    ADD COLUMN IF NOT EXISTS rejected BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_receipt_rejected ON receipt(rejected);
