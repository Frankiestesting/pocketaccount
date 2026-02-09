package com.frnholding.pocketaccount.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "corrections")
public class CorrectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String documentId;
    private String documentType;
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> fields;
    private String note;
    private Integer correctionVersion;
    private Instant savedAt;
    @Column(name = "correction_placed_at")
    private OffsetDateTime correctionPlacedAt;
    private String savedBy;
    private Integer normalizedTransactionsCreated;

    // No-argument constructor
    public CorrectionEntity() {
    }

    // All-arguments constructor
    public CorrectionEntity(Long id, String documentId, String documentType, Map<String, Object> fields,
                            String note, Integer correctionVersion, Instant savedAt,
                            OffsetDateTime correctionPlacedAt, String savedBy,
                            Integer normalizedTransactionsCreated) {
        this.id = id;
        this.documentId = documentId;
        this.documentType = documentType;
        this.fields = fields;
        this.note = note;
        this.correctionVersion = correctionVersion;
        this.savedAt = savedAt;
        this.correctionPlacedAt = correctionPlacedAt;
        this.savedBy = savedBy;
        this.normalizedTransactionsCreated = normalizedTransactionsCreated;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    // Convert to domain
    public Correction toDomain() {
        return new Correction(id, documentId, documentType, fields, note, correctionVersion, savedAt,
                correctionPlacedAt, savedBy, normalizedTransactionsCreated);
    }

    // From domain
    public static CorrectionEntity fromDomain(Correction correction) {
        return new CorrectionEntity(correction.getId(), correction.getDocumentId(), correction.getDocumentType(),
            correction.getFields(), correction.getNote(), correction.getCorrectionVersion(),
            correction.getSavedAt(), correction.getCorrectionPlacedAt(), correction.getSavedBy(),
            correction.getNormalizedTransactionsCreated());
    }
}