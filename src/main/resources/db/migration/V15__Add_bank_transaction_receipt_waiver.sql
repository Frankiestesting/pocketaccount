ALTER TABLE bank_transaction
    ADD COLUMN IF NOT EXISTS receipt_waived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS receipt_waiver_reason VARCHAR(50),
    ADD COLUMN IF NOT EXISTS receipt_waiver_note TEXT,
    ADD COLUMN IF NOT EXISTS receipt_waived_at TIMESTAMPTZ;
