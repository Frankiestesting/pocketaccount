CREATE TABLE IF NOT EXISTS correction_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255),
    snapshot JSONB NOT NULL,
    note TEXT,
    correction_version INTEGER NOT NULL,
    corrected_at TIMESTAMPTZ NOT NULL,
    corrected_by VARCHAR(255),
    CONSTRAINT fk_correction_history_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_correction_history_document_id ON correction_history(document_id);
CREATE INDEX IF NOT EXISTS idx_correction_history_version ON correction_history(document_id, correction_version);
