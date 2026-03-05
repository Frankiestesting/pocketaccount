-- Convert interpretation_jobs.id to UUID
ALTER TABLE interpretation_jobs ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE interpretation_jobs
SET id_uuid = CASE
    WHEN id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN id::uuid
    ELSE gen_random_uuid()
END
WHERE id_uuid IS NULL;

-- Convert interpretation_results.id to UUID and job_id to UUID
ALTER TABLE interpretation_results ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE interpretation_results
SET id_uuid = COALESCE(id_uuid, gen_random_uuid());

ALTER TABLE interpretation_results ADD COLUMN IF NOT EXISTS job_id_uuid UUID;
UPDATE interpretation_results ir
SET job_id_uuid = ij.id_uuid
FROM interpretation_jobs ij
WHERE ir.job_id = ij.id;

-- Convert statement_transactions.id and interpretation_result_id to UUID
ALTER TABLE statement_transactions ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE statement_transactions
SET id_uuid = COALESCE(id_uuid, gen_random_uuid());

ALTER TABLE statement_transactions ADD COLUMN IF NOT EXISTS interpretation_result_id_uuid UUID;
UPDATE statement_transactions st
SET interpretation_result_id_uuid = ir.id_uuid
FROM interpretation_results ir
WHERE st.interpretation_result_id = ir.id;

-- Convert jobs.id to UUID
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS id_uuid UUID;
UPDATE jobs
SET id_uuid = CASE
    WHEN id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN id::uuid
    ELSE gen_random_uuid()
END
WHERE id_uuid IS NULL;

-- Swap columns and constraints
ALTER TABLE interpretation_jobs DROP CONSTRAINT IF EXISTS interpretation_jobs_pkey;
ALTER TABLE interpretation_jobs DROP COLUMN id;
ALTER TABLE interpretation_jobs RENAME COLUMN id_uuid TO id;
ALTER TABLE interpretation_jobs ADD PRIMARY KEY (id);

ALTER TABLE interpretation_results DROP CONSTRAINT IF EXISTS interpretation_results_pkey;
ALTER TABLE interpretation_results DROP COLUMN id;
ALTER TABLE interpretation_results RENAME COLUMN id_uuid TO id;
ALTER TABLE interpretation_results ADD PRIMARY KEY (id);

ALTER TABLE interpretation_results DROP COLUMN job_id;
ALTER TABLE interpretation_results RENAME COLUMN job_id_uuid TO job_id;
ALTER TABLE interpretation_results ADD CONSTRAINT uk_interpretation_results_job_id UNIQUE (job_id);

ALTER TABLE statement_transactions DROP CONSTRAINT IF EXISTS fk_statement_transaction_result;
ALTER TABLE statement_transactions DROP CONSTRAINT IF EXISTS statement_transactions_pkey;
ALTER TABLE statement_transactions DROP COLUMN interpretation_result_id;
ALTER TABLE statement_transactions RENAME COLUMN interpretation_result_id_uuid TO interpretation_result_id;
ALTER TABLE statement_transactions DROP COLUMN id;
ALTER TABLE statement_transactions RENAME COLUMN id_uuid TO id;
ALTER TABLE statement_transactions ADD PRIMARY KEY (id);
ALTER TABLE statement_transactions
    ADD CONSTRAINT fk_statement_transaction_result
    FOREIGN KEY (interpretation_result_id) REFERENCES interpretation_results(id) ON DELETE CASCADE;

ALTER TABLE jobs DROP CONSTRAINT IF EXISTS jobs_pkey;
ALTER TABLE jobs DROP COLUMN id;
ALTER TABLE jobs RENAME COLUMN id_uuid TO id;
ALTER TABLE jobs ADD PRIMARY KEY (id);
