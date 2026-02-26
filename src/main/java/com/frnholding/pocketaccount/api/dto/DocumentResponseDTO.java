package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;
import java.util.UUID;

public class DocumentResponseDTO {
    private UUID id;
    private String status;
    private String documentType;
    private Instant uploadedAt;
    private String originalFilename;

    public DocumentResponseDTO() {
    }

    public DocumentResponseDTO(UUID id, String status, String documentType, Instant uploadedAt, String originalFilename) {
        this.id = id;
        this.status = status;
        this.documentType = documentType;
        this.uploadedAt = uploadedAt;
        this.originalFilename = originalFilename;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
}