package com.frnholding.pocketaccount.interpretation.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateJobDocumentTypeRequest {
    @NotBlank(message = "documentType must not be blank")
    private String documentType;

    public UpdateJobDocumentTypeRequest() {
    }

    public UpdateJobDocumentTypeRequest(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
}
