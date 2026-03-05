package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;
import java.util.UUID;

public class JobCreationResponseDTO {
    private UUID jobId;
    private UUID documentId;
    private String status;
    private Instant created;

    public JobCreationResponseDTO() {
    }

    public JobCreationResponseDTO(UUID jobId, UUID documentId, String status, Instant created) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.status = status;
        this.created = created;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
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
}