-- Convert documents.id and related document_id columns to UUID

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
