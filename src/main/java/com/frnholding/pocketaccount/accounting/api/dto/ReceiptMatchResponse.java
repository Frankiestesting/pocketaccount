package com.frnholding.pocketaccount.accounting.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ReceiptMatchResponse {
    private UUID id;
    private UUID receiptId;
    private UUID bankTransactionId;
    private BigDecimal matchedAmount;
    private String matchType;
    private BigDecimal confidence;
    private Instant createdAt;
    private String status;
    
    public ReceiptMatchResponse() {
    }
    
    public ReceiptMatchResponse(UUID id, UUID receiptId, UUID bankTransactionId, BigDecimal matchedAmount,
                               String matchType, BigDecimal confidence, Instant createdAt, String status) {
        this.id = id;
        this.receiptId = receiptId;
        this.bankTransactionId = bankTransactionId;
        this.matchedAmount = matchedAmount;
        this.matchType = matchType;
        this.confidence = confidence;
        this.createdAt = createdAt;
        this.status = status;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
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
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
