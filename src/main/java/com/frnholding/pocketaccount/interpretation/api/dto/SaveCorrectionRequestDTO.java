package com.frnholding.pocketaccount.interpretation.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for saving corrections to interpretation results.
 */
public class SaveCorrectionRequestDTO {
    /**
     * Document type being corrected.
     * Values: "INVOICE", "STATEMENT", "RECEIPT", "UNKNOWN"
     */
    private String documentType;
    
    /**
     * Corrected invoice fields (only for INVOICE type).
     */
    private InvoiceFieldsDto invoiceFields;
    
    /**
     * Corrected statement transactions (only for STATEMENT type).
     */
    private List<TransactionDto> transactions;
    
    /**
     * Optional note about the correction.
     */
    private String note;

    public SaveCorrectionRequestDTO() {
    }

    public SaveCorrectionRequestDTO(String documentType, InvoiceFieldsDto invoiceFields, 
                                    List<TransactionDto> transactions, String note) {
        this.documentType = documentType;
        this.invoiceFields = invoiceFields;
        this.transactions = transactions;
        this.note = note;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
        private Double amount;
        private String currency;
        private LocalDate date;
        private String description;

        public TransactionDto() {
        }

        public TransactionDto(Double amount, String currency, LocalDate date, String description) {
            this.amount = amount;
            this.currency = currency;
            this.date = date;
            this.description = description;
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
    }
}
