-- Ensure interpretation jobs are removed when a document is deleted
ALTER TABLE interpretation_jobs
    DROP CONSTRAINT IF EXISTS fk_interpretation_jobs_document;

ALTER TABLE interpretation_jobs
    ADD CONSTRAINT fk_interpretation_jobs_document
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

-- Ensure interpretation results are removed when a document is deleted
ALTER TABLE interpretation_results
    DROP CONSTRAINT IF EXISTS fk_interpretation_results_document;

ALTER TABLE interpretation_results
    ADD CONSTRAINT fk_interpretation_results_document
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
