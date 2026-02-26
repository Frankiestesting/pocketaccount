package com.frnholding.pocketaccount.interpretation.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for interpretation job status.
 */
public class JobStatusResponseDTO {
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
     * Document type being processed.
     * Values: "INVOICE", "STATEMENT", "RECEIPT", "UNKNOWN"
     */
    private String documentType;
    
    /**
     * Timestamp when the job was created.
     */
    private Instant created;
    
    /**
     * Timestamp when the job started processing (null if not started).
     */
    private Instant startedAt;
    
    /**
     * Timestamp when the job finished (null if not finished).
     */
    private Instant finishedAt;
    
    /**
     * Error message if the job failed (null if no error).
     */
    private String error;
    
    /**
     * Original filename of the document being interpreted.
     */
    private String originalFilename;
    
    /**
     * Extraction methods used for interpretation (e.g., "PDFBox, AIStatementExtractor").
     */
    private String extractionMethods;

    public JobStatusResponseDTO() {
    }

    public JobStatusResponseDTO(String jobId, UUID documentId, String status, String documentType, 
                                Instant created, Instant startedAt, Instant finishedAt, String error, 
                                String originalFilename, String extractionMethods) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.status = status;
        this.documentType = documentType;
        this.created = created;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.error = error;
        this.originalFilename = originalFilename;
        this.extractionMethods = extractionMethods;
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

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getExtractionMethods() {
        return extractionMethods;
    }

    public void setExtractionMethods(String extractionMethods) {
        this.extractionMethods = extractionMethods;
    }
}
