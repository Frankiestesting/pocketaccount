package com.frnholding.pocketaccount.accounting.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateReceiptMatchRequest {
    @NotNull(message = "receiptId must not be null")
    private UUID receiptId;
    
    @NotNull(message = "bankTransactionId must not be null")
    private UUID bankTransactionId;
    
    @NotNull(message = "matchedAmount must not be null")
    @DecimalMin(value = "0.01", message = "matchedAmount must be greater than 0")
    private BigDecimal matchedAmount;
    
    @NotNull(message = "matchType must not be null")
    @Pattern(regexp = "AUTO|MANUAL", message = "matchType must be either AUTO or MANUAL")
    private String matchType;
    
    @DecimalMin(value = "0", message = "confidence must be between 0 and 1")
    @DecimalMax(value = "1", message = "confidence must be between 0 and 1")
    private BigDecimal confidence;
    
    public CreateReceiptMatchRequest() {
    }
    
    public CreateReceiptMatchRequest(UUID receiptId, UUID bankTransactionId, BigDecimal matchedAmount,
                                     String matchType, BigDecimal confidence) {
        this.receiptId = receiptId;
        this.bankTransactionId = bankTransactionId;
        this.matchedAmount = matchedAmount;
        this.matchType = matchType;
        this.confidence = confidence;
    }
    
    public UUID getReceiptId() {
        return receiptId;
    }
    
    public void setReceiptId(UUID receiptId) {
        this.receiptId = receiptId;
    }
    
    public UUID getBankTransactionId() {
        return bankTransactionId;
    }
    
    public void setBankTransactionId(UUID bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
    }
    
    public BigDecimal getMatchedAmount() {
        return matchedAmount;
    }
    
    public void setMatchedAmount(BigDecimal matchedAmount) {
        this.matchedAmount = matchedAmount;
    }
    
    public String getMatchType() {
        return matchType;
    }
    
    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }
    
    public BigDecimal getConfidence() {
        return confidence;
    }
    
    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }
}
