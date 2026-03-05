package com.frnholding.pocketaccount.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateDocumentTypeRequestDTO {
    @NotBlank(message = "documentType must not be blank")
    private String documentType;

    public UpdateDocumentTypeRequestDTO() {
    }

    public UpdateDocumentTypeRequestDTO(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
}
