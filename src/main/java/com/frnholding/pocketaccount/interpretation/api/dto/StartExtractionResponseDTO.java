package com.frnholding.pocketaccount.interpretation.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO after starting a document interpretation job.
 */
public class StartExtractionResponseDTO {
    /**
     * Unique identifier for the interpretation job.
     */
    private String jobId;
    
    /**
     * Document ID being interpreted.
     */
    private UUID documentId;
    
    /**
     * Current status of the job.
     * Values: "PENDING", "RUNNING", "COMPLETED", "FAILED", "CANCELLED"
     */
    private String status;
    
    /**
     * Timestamp when the job was created.
     */
    private Instant created;
    
    /**
     * Document type (if detected or hinted).
     * Values: "INVOICE", "STATEMENT", "RECEIPT", "UNKNOWN"
     */
    private String documentType;

    public StartExtractionResponseDTO() {
    }

    public StartExtractionResponseDTO(String jobId, UUID documentId, String status, 
                                      Instant created, String documentType) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.status = status;
        this.created = created;
        this.documentType = documentType;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
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

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
}
