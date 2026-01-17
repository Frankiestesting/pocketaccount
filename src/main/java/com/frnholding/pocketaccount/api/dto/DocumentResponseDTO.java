package com.frnholding.pocketaccount.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class DocumentResponseDTO {
    private String id;
    private String status;
    private String documentType;
    private Instant uploadedAt;
    private String originalFilename;

    public DocumentResponseDTO(String id, String status, String documentType, Instant uploadedAt, String originalFilename) {
        this.id = id;
        this.status = status;
        this.documentType = documentType;
        this.uploadedAt = uploadedAt;
        this.originalFilename = originalFilename;
    }
}