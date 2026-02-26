package com.frnholding.pocketaccount.domain;

import java.time.Instant;
import java.util.UUID;

public class Job {
    private String id;
    private UUID documentId;
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
    public Job() {
    }

    // All-arguments constructor
    public Job(String id, UUID documentId, String status, Instant created, String pipeline, 
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;

        if (useOcr != job.useOcr) return false;
        if (useAi != job.useAi) return false;
        if (!id.equals(job.id)) return false;
        if (!documentId.equals(job.documentId)) return false;
        if (!status.equals(job.status)) return false;
        if (!created.equals(job.created)) return false;
        if (!pipeline.equals(job.pipeline)) return false;
        if (!languageHint.equals(job.languageHint)) return false;
        if (!startedAt.equals(job.startedAt)) return false;
        if (!finishedAt.equals(job.finishedAt)) return false;
        return error.equals(job.error);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + documentId.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + created.hashCode();
        result = 31 * result + pipeline.hashCode();
        result = 31 * result + (useOcr ? 1 : 0);
        result = 31 * result + (useAi ? 1 : 0);
        result = 31 * result + languageHint.hashCode();
        result = 31 * result + startedAt.hashCode();
        result = 31 * result + finishedAt.hashCode();
        result = 31 * result + error.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", documentId='" + documentId + '\'' +
                ", status='" + status + '\'' +
                ", created=" + created +
                ", pipeline='" + pipeline + '\'' +
                ", useOcr=" + useOcr +
                ", useAi=" + useAi +
                ", languageHint='" + languageHint + '\'' +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", error='" + error + '\'' +
                '}';
    }
}