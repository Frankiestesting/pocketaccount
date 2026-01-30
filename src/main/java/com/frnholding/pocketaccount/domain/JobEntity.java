package com.frnholding.pocketaccount.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id
    private String id;
    private String documentId;
    private String status;
    private Instant created;
    private String pipeline;
    private boolean useOcr;
    private boolean useAi;
    private String languageHint;
    private Instant startedAt;
    private Instant finishedAt;
    private String error;

    // No-argument constructor
    public JobEntity() {
    }

    // All-arguments constructor
    public JobEntity(String id, String documentId, String status, Instant created, String pipeline, 
                     boolean useOcr, boolean useAi, String languageHint, Instant startedAt, 
                     Instant finishedAt, String error) {
        this.id = id;
        this.documentId = documentId;
        this.status = status;
        this.created = created;
        this.pipeline = pipeline;
        this.useOcr = useOcr;
        this.useAi = useAi;
        this.languageHint = languageHint;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.error = error;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isUseOcr() {
        return useOcr;
    }

    public void setUseOcr(boolean useOcr) {
        this.useOcr = useOcr;
    }

    public boolean isUseAi() {
        return useAi;
    }

    public void setUseAi(boolean useAi) {
        this.useAi = useAi;
    }

    public String getLanguageHint() {
        return languageHint;
    }

    public void setLanguageHint(String languageHint) {
        this.languageHint = languageHint;
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

    // Convert to domain
    public Job toDomain() {
        return new Job(id, documentId, status, created, pipeline, useOcr, useAi, languageHint, startedAt, finishedAt, error);
    }

    // From domain
    public static JobEntity fromDomain(Job job) {
        return new JobEntity(job.getId(), job.getDocumentId(), job.getStatus(), job.getCreated(),
                           job.getPipeline(), job.isUseOcr(), job.isUseAi(), job.getLanguageHint(),
                           job.getStartedAt(), job.getFinishedAt(), job.getError());
    }
}