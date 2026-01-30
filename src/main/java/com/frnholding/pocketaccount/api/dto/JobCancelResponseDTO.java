package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;

public class JobCancelResponseDTO {
    private String jobId;
    private String status;
    private Instant cancelledAt;

    public JobCancelResponseDTO() {
    }

    public JobCancelResponseDTO(String jobId, String status, Instant cancelledAt) {
        this.jobId = jobId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
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