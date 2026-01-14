package com.frnholding.pocketaccount.interpretation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for saving corrections to interpretation results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveCorrectionRequest {
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
