package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;

public class JobCreationResponseDTO {
    private String jobId;
    private String documentId;
    private String status;
    private Instant created;

    public JobCreationResponseDTO() {
    }

    public JobCreationResponseDTO(String jobId, String documentId, String status, Instant created) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.status = status;
        this.created = created;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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
}