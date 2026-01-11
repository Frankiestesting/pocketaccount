package com.frnholding.pocketaccount.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {
    @Id
    private String id;
    private String status;
    private Instant created;
    private String originalFilename;
    private String filePath;
    private String documentType;

    // Convert to domain
    public Document toDomain() {
        return new Document(id, status, created, originalFilename, filePath, documentType);
    }

    // From domain
    public static DocumentEntity fromDomain(Document document) {
        return new DocumentEntity(document.getId(), document.getStatus(), document.getCreated(), document.getOriginalFilename(), document.getFilePath(), document.getDocumentType());
    }
}