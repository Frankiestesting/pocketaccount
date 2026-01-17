package com.frnholding.pocketaccount.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponseDTO {
    private String documentId;
    private String status;
    private Instant created;
    private String originalFilename;
    private String documentType;
    private Long fileSize;
}