package com.frnholding.pocketaccount.accounting.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CreateReceiptRequest {
    @NotNull(message = "documentId must not be null")
    private UUID documentId;
    
    private LocalDate purchaseDate;
    
    @NotNull(message = "totalAmount must not be null")
    @DecimalMin(value = "0.01", message = "totalAmount must be positive")
    private BigDecimal totalAmount;
    
    @NotNull(message = "currency must not be null")
    @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code (e.g., USD, EUR)")
    private String currency;
    
    @Size(max = 200, message = "merchant must not exceed 200 characters")
    private String merchant;
    
    private String description;
    
    public CreateReceiptRequest() {
    }
    
    public CreateReceiptRequest(UUID documentId, LocalDate purchaseDate, BigDecimal totalAmount,
                               String currency, String merchant, String description) {
        this.documentId = documentId;
        this.purchaseDate = purchaseDate;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.merchant = merchant;
        this.description = description;
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
}
