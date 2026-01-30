package com.frnholding.pocketaccount.accounting.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BankStatementLineDto {
    
    private LocalDate bookingDate;
    
    private LocalDate valueDate;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "-999999999999.99", message = "Amount must be a valid number")
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO code (e.g., USD, EUR, NOK)")
    private String currency;
    
    private String counterparty;
    
    private String description;
    
    private String reference;
    
    @NotNull(message = "Source line hash is required")
    @Size(min = 64, max = 64, message = "Source line hash must be exactly 64 characters")
    private String sourceLineHash;
    
    public BankStatementLineDto() {
    }
    
    public LocalDate getBookingDate() {
        return bookingDate;
    }
    
    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public LocalDate getValueDate() {
        return valueDate;
    }
    
    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getCounterparty() {
        return counterparty;
    }
    
    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public String getSourceLineHash() {
        return sourceLineHash;
    }
    
    public void setSourceLineHash(String sourceLineHash) {
        this.sourceLineHash = sourceLineHash;
    }
}
