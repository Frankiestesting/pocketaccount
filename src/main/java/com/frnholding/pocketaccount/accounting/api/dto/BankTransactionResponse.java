package com.frnholding.pocketaccount.accounting.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class BankTransactionResponse {
    private UUID id;
    private UUID accountId;
    private LocalDate bookingDate;
    private LocalDate valueDate;
    private BigDecimal amount;
    private String currency;
    private String counterparty;
    private String description;
    private String reference;
    private UUID sourceDocumentId;
    private String sourceLineHash;
    private Instant createdAt;
    
    public BankTransactionResponse() {
    }
    
    public BankTransactionResponse(UUID id, UUID accountId, LocalDate bookingDate, LocalDate valueDate,
                                   BigDecimal amount, String currency, String counterparty, String description,
                                   String reference, UUID sourceDocumentId, String sourceLineHash, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.bookingDate = bookingDate;
        this.valueDate = valueDate;
        this.amount = amount;
        this.currency = currency;
        this.counterparty = counterparty;
        this.description = description;
        this.reference = reference;
        this.sourceDocumentId = sourceDocumentId;
        this.sourceLineHash = sourceLineHash;
        this.createdAt = createdAt;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
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
    
    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }
    
    public void setSourceDocumentId(UUID sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }
    
    public String getSourceLineHash() {
        return sourceLineHash;
    }
    
    public void setSourceLineHash(String sourceLineHash) {
        this.sourceLineHash = sourceLineHash;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
