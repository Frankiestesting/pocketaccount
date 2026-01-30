package com.frnholding.pocketaccount.api.dto;

import java.util.Map;

public class DocumentCorrectionRequestDTO {
    private String documentType;
    private Map<String, Object> fields;
    private String note;

    public DocumentCorrectionRequestDTO() {
    }

    public DocumentCorrectionRequestDTO(String documentType, Map<String, Object> fields, String note) {
        this.documentType = documentType;
        this.fields = fields;
        this.note = note;
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
}