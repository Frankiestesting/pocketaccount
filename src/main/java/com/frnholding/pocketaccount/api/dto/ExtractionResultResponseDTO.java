package com.frnholding.pocketaccount.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.List;

public class ExtractionResultResponseDTO {
    private String documentId;
    private String documentType;
    private Integer extractionVersion;
    private Instant extractedAt;
    private Map<String, Object> fields;
    private Map<String, Object> correctedFields;
    private Map<String, Double> confidence;
    private List<String> warnings;
    private List<Transaction> transactions;

    public ExtractionResultResponseDTO() {
    }

    public ExtractionResultResponseDTO(String documentId, String documentType, Integer extractionVersion,
                                       Instant extractedAt, Map<String, Object> fields,
                                       Map<String, Object> correctedFields,
                                       Map<String, Double> confidence, List<String> warnings,
                                       List<Transaction> transactions) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.extractionVersion = extractionVersion;
        this.extractedAt = extractedAt;
        this.fields = fields;
        this.correctedFields = correctedFields;
        this.confidence = confidence;
        this.warnings = warnings;
        this.transactions = transactions;
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

    public Integer getExtractionVersion() {
        return extractionVersion;
    }

    public void setExtractionVersion(Integer extractionVersion) {
        this.extractionVersion = extractionVersion;
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(Instant extractedAt) {
        this.extractedAt = extractedAt;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public Map<String, Object> getCorrectedFields() {
        return correctedFields;
    }

    public void setCorrectedFields(Map<String, Object> correctedFields) {
        this.correctedFields = correctedFields;
    }

    public Map<String, Double> getConfidence() {
        return confidence;
    }

    public void setConfidence(Map<String, Double> confidence) {
        this.confidence = confidence;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
    
    public static class Transaction {
        private String date;
        private Double amount;
        private String currency;
        private String description;
        private Map<String, Double> confidence;

        public Transaction() {
        }

        public Transaction(String date, Double amount, String currency, String description, 
                          Map<String, Double> confidence) {
            this.date = date;
            this.amount = amount;
            this.currency = currency;
            this.description = description;
            this.confidence = confidence;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Double> getConfidence() {
            return confidence;
        }

        public void setConfidence(Map<String, Double> confidence) {
            this.confidence = confidence;
        }
    }
}