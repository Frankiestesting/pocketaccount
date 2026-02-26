package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DocumentCorrectionResponseDTO {
    private UUID documentId;
    private Integer correctionVersion;
    private Instant savedAt;
    private OffsetDateTime correctionPlacedAt;
    private String savedBy;
    private Integer normalizedTransactionsCreated;

    public DocumentCorrectionResponseDTO() {
    }

    public DocumentCorrectionResponseDTO(UUID documentId, Integer correctionVersion, Instant savedAt,
                                         OffsetDateTime correctionPlacedAt, String savedBy,
                                         Integer normalizedTransactionsCreated) {
        this.documentId = documentId;
        this.correctionVersion = correctionVersion;
        this.savedAt = savedAt;
        this.correctionPlacedAt = correctionPlacedAt;
        this.savedBy = savedBy;
        this.normalizedTransactionsCreated = normalizedTransactionsCreated;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public Integer getCorrectionVersion() {
        return correctionVersion;
    }

    public void setCorrectionVersion(Integer correctionVersion) {
        this.correctionVersion = correctionVersion;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Instant savedAt) {
        this.savedAt = savedAt;
    }

    public OffsetDateTime getCorrectionPlacedAt() {
        return correctionPlacedAt;
    }

    public void setCorrectionPlacedAt(OffsetDateTime correctionPlacedAt) {
        this.correctionPlacedAt = correctionPlacedAt;
    }

    public String getSavedBy() {
        return savedBy;
    }

    public void setSavedBy(String savedBy) {
        this.savedBy = savedBy;
    }

    public Integer getNormalizedTransactionsCreated() {
        return normalizedTransactionsCreated;
    }

    public void setNormalizedTransactionsCreated(Integer normalizedTransactionsCreated) {
        this.normalizedTransactionsCreated = normalizedTransactionsCreated;
    }
}