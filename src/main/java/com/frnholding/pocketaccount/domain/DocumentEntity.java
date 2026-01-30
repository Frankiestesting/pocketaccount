package com.frnholding.pocketaccount.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id
    private String id;
    private String status;
    private Instant created;
    private String originalFilename;
    private String filePath;
    private String documentType;

    // No-argument constructor
    public DocumentEntity() {
    }

    // All-arguments constructor
    public DocumentEntity(String id, String status, Instant created, String originalFilename, 
                          String filePath, String documentType) {
        this.id = id;
        this.status = status;
        this.created = created;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
        this.documentType = documentType;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    // Convert to domain
    public Document toDomain() {
        return new Document(id, status, created, originalFilename, filePath, documentType);
    }

    // From domain
    public static DocumentEntity fromDomain(Document document) {
        return new DocumentEntity(document.getId(), document.getStatus(), document.getCreated(), 
                document.getOriginalFilename(), document.getFilePath(), document.getDocumentType());
    }
}

