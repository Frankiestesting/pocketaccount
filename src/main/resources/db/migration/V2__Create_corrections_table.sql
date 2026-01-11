CREATE TABLE corrections (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(255),
    fields JSONB,
    note TEXT,
    correction_version INTEGER NOT NULL,
    saved_at TIMESTAMP NOT NULL,
    saved_by VARCHAR(255),
    normalized_transactions_created INTEGER,
    FOREIGN KEY (document_id) REFERENCES documents(id)
);