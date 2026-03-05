package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;
import java.util.UUID;

public class JobCancelResponseDTO {
    private UUID jobId;
    private String status;
    private Instant cancelledAt;

    public JobCancelResponseDTO() {
    }

    public JobCancelResponseDTO(UUID jobId, String status, Instant cancelledAt) {
        this.jobId = jobId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}