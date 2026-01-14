package com.frnholding.pocketaccount.interpretation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for interpretation/extraction results.
 * Contains either invoice fields or statement transactions based on document type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResultResponse {
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
     * Invoice fields (populated only for INVOICE document type).
     */
    private InvoiceFieldsDto invoiceFields;
    
    /**
     * Statement transactions (populated only for STATEMENT document type).
     */
    private List<TransactionDto> transactions;
    
    /**
     * DTO for invoice-specific fields.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceFieldsDto {
        private Double amount;
        private String currency;
        private LocalDate date;
        private String description;
        private String sender;
    }
    
    /**
     * DTO for statement transaction.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDto {
        private Double amount;
        private String currency;
        private LocalDate date;
        private String description;
    }
}
