DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'statement_transactions'::regclass
          AND contype = 'f'
          AND confrelid = 'interpretation_results'::regclass
    LOOP
        EXECUTE format('ALTER TABLE statement_transactions DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE statement_transactions
    ADD CONSTRAINT fk_statement_transaction_result
    FOREIGN KEY (interpretation_result_id) REFERENCES interpretation_results(id) ON DELETE CASCADE;
