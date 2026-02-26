package com.frnholding.pocketaccount.interpretation.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "correction_history")
public class CorrectionHistoryEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(columnDefinition = "uuid", nullable = false)
    private UUID documentId;

    private String documentType;

    @Column(nullable = false)
    private String entityType;

    private String entityId;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> snapshot;

    private String note;

    @Column(nullable = false)
    private Integer correctionVersion;

    @Column(nullable = false)
    private OffsetDateTime correctedAt;

    private String correctedBy;

    public CorrectionHistoryEntity() {
    }

    public CorrectionHistoryEntity(UUID id, UUID documentId, String documentType, String entityType,
                                   String entityId, Map<String, Object> snapshot, String note,
                                   Integer correctionVersion, OffsetDateTime correctedAt, String correctedBy) {
        this.id = id;
        this.documentId = documentId;
        this.documentType = documentType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.snapshot = snapshot;
        this.note = note;
        this.correctionVersion = correctionVersion;
        this.correctedAt = correctedAt;
        this.correctedBy = correctedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Map<String, Object> getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Map<String, Object> snapshot) {
        this.snapshot = snapshot;
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

    public OffsetDateTime getCorrectedAt() {
        return correctedAt;
    }

    public void setCorrectedAt(OffsetDateTime correctedAt) {
        this.correctedAt = correctedAt;
    }

    public String getCorrectedBy() {
        return correctedBy;
    }

    public void setCorrectedBy(String correctedBy) {
        this.correctedBy = correctedBy;
    }
}
