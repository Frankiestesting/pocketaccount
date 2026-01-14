package com.frnholding.pocketaccount.interpretation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO after starting a document interpretation job.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartExtractionResponse {
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
     * Timestamp when the job was created.
     */
    private Instant created;
    
    /**
     * Document type (if detected or hinted).
     * Values: "INVOICE", "STATEMENT", "RECEIPT", "UNKNOWN"
     */
    private String documentType;
}
