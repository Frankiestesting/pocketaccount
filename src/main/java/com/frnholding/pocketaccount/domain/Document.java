package com.frnholding.pocketaccount.domain;

import java.time.Instant;
import java.util.UUID;

public class Document {
    private UUID id;
    private String status;
    private Instant created;
    private String originalFilename;
    private String filePath;
    private String documentType;

    // No-argument constructor
    public Document() {
    }

    // All-arguments constructor
    public Document(UUID id, String status, Instant created, String originalFilename, 
                    String filePath, String documentType) {
        this.id = id;
        this.status = status;
        this.created = created;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
        this.documentType = documentType;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Document document = (Document) o;

        if (!id.equals(document.id)) return false;
        if (!status.equals(document.status)) return false;
        if (!created.equals(document.created)) return false;
        if (!originalFilename.equals(document.originalFilename)) return false;
        if (!filePath.equals(document.filePath)) return false;
        return documentType.equals(document.documentType);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + created.hashCode();
        result = 31 * result + originalFilename.hashCode();
        result = 31 * result + filePath.hashCode();
        result = 31 * result + documentType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Document{" +
            "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", created=" + created +
                ", originalFilename='" + originalFilename + '\'' +
                ", filePath='" + filePath + '\'' +
                ", documentType='" + documentType + '\'' +
                '}';
    }
}