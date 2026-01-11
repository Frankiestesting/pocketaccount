CREATE TABLE documents (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255),
    created TIMESTAMP,
    original_filename VARCHAR(255),
    file_path VARCHAR(255),
    document_type VARCHAR(255)
);