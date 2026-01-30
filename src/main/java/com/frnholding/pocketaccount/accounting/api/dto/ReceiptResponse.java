package com.frnholding.pocketaccount.accounting.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ReceiptResponse {
    private UUID id;
    private UUID documentId;
    private LocalDate purchaseDate;
    private BigDecimal totalAmount;
    private String currency;
    private String merchant;
    private String description;
    private Instant createdAt;
    
    public ReceiptResponse() {
    }
    
    public ReceiptResponse(UUID id, UUID documentId, LocalDate purchaseDate, BigDecimal totalAmount,
                          String currency, String merchant, String description, Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.purchaseDate = purchaseDate;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.merchant = merchant;
        this.description = description;
        this.createdAt = createdAt;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
    
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getMerchant() {
        return merchant;
    }
    
    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
