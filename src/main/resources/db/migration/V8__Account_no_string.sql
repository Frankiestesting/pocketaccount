ALTER TABLE account
    ALTER COLUMN account_no TYPE VARCHAR(11)
    USING LPAD(account_no::text, 11, '0');

ALTER TABLE statement_transactions
    ALTER COLUMN account_no TYPE VARCHAR(11)
    USING LPAD(account_no::text, 11, '0');
