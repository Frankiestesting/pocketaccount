package com.frnholding.pocketaccount.domain;

import java.time.Instant;
import java.util.Map;

public class Correction {
    private Long id;
    private String documentId;
    private String documentType;
    private Map<String, Object> fields;
    private String note;
    private Integer correctionVersion;
    private Instant savedAt;
    private String savedBy;
    private Integer normalizedTransactionsCreated;
    
    // No-argument constructor
    public Correction() {
    }

    // All-arguments constructor
    public Correction(Long id, String documentId, String documentType, Map<String, Object> fields, 
                      String note, Integer correctionVersion, Instant savedAt, String savedBy, 
                      Integer normalizedTransactionsCreated) {
        this.id = id;
        this.documentId = documentId;
        this.documentType = documentType;
        this.fields = fields;
        this.note = note;
        this.correctionVersion = correctionVersion;
        this.savedAt = savedAt;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Correction that = (Correction) o;

        if (!id.equals(that.id)) return false;
        if (!documentId.equals(that.documentId)) return false;
        if (!documentType.equals(that.documentType)) return false;
        if (!fields.equals(that.fields)) return false;
        if (!note.equals(that.note)) return false;
        if (!correctionVersion.equals(that.correctionVersion)) return false;
        if (!savedAt.equals(that.savedAt)) return false;
        if (!savedBy.equals(that.savedBy)) return false;
        return normalizedTransactionsCreated.equals(that.normalizedTransactionsCreated);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + documentId.hashCode();
        result = 31 * result + documentType.hashCode();
        result = 31 * result + fields.hashCode();
        result = 31 * result + note.hashCode();
        result = 31 * result + correctionVersion.hashCode();
        result = 31 * result + savedAt.hashCode();
        result = 31 * result + savedBy.hashCode();
        result = 31 * result + normalizedTransactionsCreated.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Correction{" +
                "id=" + id +
                ", documentId='" + documentId + '\'' +
                ", documentType='" + documentType + '\'' +
                ", fields=" + fields +
                ", note='" + note + '\'' +
                ", correctionVersion=" + correctionVersion +
                ", savedAt=" + savedAt +
                ", savedBy='" + savedBy + '\'' +
                ", normalizedTransactionsCreated=" + normalizedTransactionsCreated +
                '}';
    }
}