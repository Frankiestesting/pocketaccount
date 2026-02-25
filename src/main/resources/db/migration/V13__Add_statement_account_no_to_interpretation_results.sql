ALTER TABLE interpretation_results
    ADD COLUMN IF NOT EXISTS account_no VARCHAR(11);
