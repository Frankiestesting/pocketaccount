package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;

public class DocumentUploadResponseDTO {
    private String documentId;
    private String status;
    private Instant created;
    private String originalFilename;
    private String documentType;
    private Long fileSize;

    public DocumentUploadResponseDTO() {
    }

    public DocumentUploadResponseDTO(String documentId, String status, Instant created, 
                                     String originalFilename, String documentType, Long fileSize) {
        this.documentId = documentId;
        this.status = status;
        this.created = created;
        this.originalFilename = originalFilename;
        this.documentType = documentType;
        this.fileSize = fileSize;
    }

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

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}