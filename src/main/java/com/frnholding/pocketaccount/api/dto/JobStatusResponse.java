package com.frnholding.pocketaccount.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponse {
    private String jobId;
    private String documentId;
    private String status;
    private Instant startedAt;
    private Instant finishedAt;
    private String error;
    private String documentType;
    private String originalFilename;

    public JobStatusResponse(String jobId, String documentId, String status, Instant startedAt, Instant finishedAt, String error) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.error = error;
    }
}