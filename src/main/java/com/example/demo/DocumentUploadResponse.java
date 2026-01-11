package com.example.demo;

import java.time.Instant;

public class DocumentUploadResponse {
    private String documentId;
    private String status;
    private Instant created;
    private String originalFilename;

    public DocumentUploadResponse() {}

    public DocumentUploadResponse(String documentId, String status, Instant created, String originalFilename) {
        this.documentId = documentId;
        this.status = status;
        this.created = created;
        this.originalFilename = originalFilename;
    }

    // Getters and setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
}