package com.frnholding.pocketaccount;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResultResponse {
    private String documentId;
    private String documentType;
    private Integer extractionVersion;
    private Instant extractedAt;
    private Map<String, Object> fields;
    private Map<String, Double> confidence;
    private List<String> warnings;
    private List<Transaction> transactions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Transaction {
        private String date;
        private Double amount;
        private String currency;
        private String description;
        private Map<String, Double> confidence;
    }
}