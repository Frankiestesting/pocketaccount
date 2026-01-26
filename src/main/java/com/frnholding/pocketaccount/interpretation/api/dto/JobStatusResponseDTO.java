package com.frnholding.pocketaccount.interpretation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for interpretation job status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponseDTO {
    /**
     * Unique identifier for the interpretation job.
     */
    private String jobId;
    
    /**
     * Document ID being interpreted.
     */
    private String documentId;
    
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
}
