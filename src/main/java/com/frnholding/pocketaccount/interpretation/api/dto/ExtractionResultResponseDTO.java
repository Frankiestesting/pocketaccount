package com.frnholding.pocketaccount.interpretation.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for interpretation/extraction results.
 * Contains either invoice fields or statement transactions based on document type.
 */
public class ExtractionResultResponseDTO {
    /**
     * Document ID that was interpreted.
     */
    private String documentId;
    
    /**
     * Type of document interpreted.
     * Values: "INVOICE", "STATEMENT", "RECEIPT", "UNKNOWN"
     */
    private String documentType;
    
    /**
     * Timestamp when the interpretation was performed.
     */
    private Instant interpretedAt;
    
    /**
     * Extraction methods used (e.g., "PDFBox, HeuristicInvoiceExtractor" or "Tesseract, AIInvoiceExtractor").
     */
    private String extractionMethods;

    /**
     * Statement account number (common for the full statement).
     */
    private String accountNo;
    
    /**
     * Invoice fields (populated only for INVOICE document type).
     */
    private InvoiceFieldsDto invoiceFields;
    
    /**
     * Statement transactions (populated only for STATEMENT document type).
     */
    private List<TransactionDto> transactions;

    public ExtractionResultResponseDTO() {
    }

    public ExtractionResultResponseDTO(String documentId, String documentType, Instant interpretedAt, 
                                       String extractionMethods, InvoiceFieldsDto invoiceFields, 
                                       List<TransactionDto> transactions) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.interpretedAt = interpretedAt;
        this.extractionMethods = extractionMethods;
        this.invoiceFields = invoiceFields;
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

    public Instant getInterpretedAt() {
        return interpretedAt;
    }

    public void setInterpretedAt(Instant interpretedAt) {
        this.interpretedAt = interpretedAt;
    }

    public String getExtractionMethods() {
        return extractionMethods;
    }

    public void setExtractionMethods(String extractionMethods) {
        this.extractionMethods = extractionMethods;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public InvoiceFieldsDto getInvoiceFields() {
        return invoiceFields;
    }

    public void setInvoiceFields(InvoiceFieldsDto invoiceFields) {
        this.invoiceFields = invoiceFields;
    }

    public List<TransactionDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDto> transactions) {
        this.transactions = transactions;
    }
    
    /**
     * DTO for invoice-specific fields.
     */
    public static class InvoiceFieldsDto {
        private Double amount;
        private String currency;
        private LocalDate date;
        private String description;
        private String sender;

        public InvoiceFieldsDto() {
        }

        public InvoiceFieldsDto(Double amount, String currency, LocalDate date, String description, String sender) {
            this.amount = amount;
            this.currency = currency;
            this.date = date;
            this.description = description;
            this.sender = sender;
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

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }
    }
    
    /**
     * DTO for statement transaction.
     */
    public static class TransactionDto {
        private Long id;
        private Double amount;
        private String currency;
        private LocalDate date;
        private String description;
        private String accountNo;
        private Boolean approved;

        public TransactionDto() {
        }

        public TransactionDto(Long id, Double amount, String currency, LocalDate date, String description,
                             String accountNo, Boolean approved) {
            this.id = id;
            this.amount = amount;
            this.currency = currency;
            this.date = date;
            this.description = description;
            this.accountNo = accountNo;
            this.approved = approved;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public void setAccountNo(String accountNo) {
            this.accountNo = accountNo;
        }

        public Boolean getApproved() {
            return approved;
        }

        public void setApproved(Boolean approved) {
            this.approved = approved;
        }
    }
}
