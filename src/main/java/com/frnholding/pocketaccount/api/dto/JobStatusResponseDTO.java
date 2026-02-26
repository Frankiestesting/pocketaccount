package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;
import java.util.UUID;

public class JobStatusResponseDTO {
    private String jobId;
    private UUID documentId;
    private String status;
    private Instant startedAt;
    private Instant finishedAt;
    private String error;
    private String documentType;
    private String originalFilename;
    private String pipeline;
    private boolean useAi;

    public JobStatusResponseDTO() {
    }

    public JobStatusResponseDTO(String jobId, UUID documentId, String status, Instant startedAt, Instant finishedAt, String error) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.error = error;
    }

    public JobStatusResponseDTO(String jobId, UUID documentId, String status, Instant startedAt, 
                                Instant finishedAt, String error, String documentType, 
                                String originalFilename, String pipeline, boolean useAi) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.error = error;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.pipeline = pipeline;
        this.useAi = useAi;
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

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isUseAi() {
        return useAi;
    }

    public void setUseAi(boolean useAi) {
        this.useAi = useAi;
    }
}