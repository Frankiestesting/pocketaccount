-- Manual migration: convert documents.id and related document_id columns to UUID
-- Run in a transaction. Make a backup first.

BEGIN;

-- Optional sanity check: find non-UUID values before running the conversion.
-- If any rows are returned, fix them before continuing.
-- SELECT id FROM documents WHERE id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
-- SELECT document_id FROM interpretation_jobs WHERE document_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
-- SELECT document_id FROM interpretation_results WHERE document_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
-- SELECT document_id FROM correction_history WHERE document_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

ALTER TABLE correction_history
    DROP CONSTRAINT IF EXISTS fk_correction_history_document;

ALTER TABLE interpretation_jobs
    DROP CONSTRAINT IF EXISTS fk_interpretation_jobs_document;

ALTER TABLE interpretation_results
    DROP CONSTRAINT IF EXISTS fk_interpretation_results_document;

ALTER TABLE documents
    ALTER COLUMN id TYPE UUID USING id::uuid;

ALTER TABLE correction_history
    ALTER COLUMN document_id TYPE UUID USING document_id::uuid;

ALTER TABLE interpretation_jobs
    ALTER COLUMN document_id TYPE UUID USING document_id::uuid;

ALTER TABLE interpretation_results
    ALTER COLUMN document_id TYPE UUID USING document_id::uuid;

ALTER TABLE jobs
    ALTER COLUMN document_id TYPE UUID USING document_id::uuid;

ALTER TABLE correction_history
    ADD CONSTRAINT fk_correction_history_document FOREIGN KEY (document_id)
    REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE interpretation_jobs
    ADD CONSTRAINT fk_interpretation_jobs_document FOREIGN KEY (document_id)
    REFERENCES documents(id) ON DELETE CASCADE;

ALTER TABLE interpretation_results
    ADD CONSTRAINT fk_interpretation_results_document FOREIGN KEY (document_id)
    REFERENCES documents(id) ON DELETE CASCADE;

COMMIT;
