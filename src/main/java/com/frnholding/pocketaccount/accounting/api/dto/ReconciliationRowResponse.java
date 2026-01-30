package com.frnholding.pocketaccount.accounting.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ReconciliationRowResponse {
    private UUID transactionId;
    private LocalDate bookingDate;
    private BigDecimal amount;
    private String description;
    private BigDecimal sumMatched;
    private String status;
    
    public ReconciliationRowResponse() {
    }
    
    public ReconciliationRowResponse(UUID transactionId, LocalDate bookingDate, BigDecimal amount,
                                    String description, BigDecimal sumMatched, String status) {
        this.transactionId = transactionId;
        this.bookingDate = bookingDate;
        this.amount = amount;
        this.description = description;
        this.sumMatched = sumMatched;
        this.status = status;
    }
    
    public UUID getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }
    
    public LocalDate getBookingDate() {
        return bookingDate;
    }
    
    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getSumMatched() {
        return sumMatched;
    }
    
    public void setSumMatched(BigDecimal sumMatched) {
        this.sumMatched = sumMatched;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
