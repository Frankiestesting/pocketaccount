package com.frnholding.pocketaccount.accounting.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ReceiptMatchCandidateResponse {
    private UUID bankTransactionId;
    private UUID accountId;
    private LocalDate bookingDate;
    private BigDecimal amount;
    private String currency;
    private String description;
    private Integer matchPrediction;

    public ReceiptMatchCandidateResponse() {
    }

    public ReceiptMatchCandidateResponse(UUID bankTransactionId, UUID accountId, LocalDate bookingDate,
                                         BigDecimal amount, String currency, String description,
                                         Integer matchPrediction) {
        this.bankTransactionId = bankTransactionId;
        this.accountId = accountId;
        this.bookingDate = bookingDate;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.matchPrediction = matchPrediction;
    }

    public UUID getBankTransactionId() {
        return bankTransactionId;
    }

    public void setBankTransactionId(UUID bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMatchPrediction() {
        return matchPrediction;
    }

    public void setMatchPrediction(Integer matchPrediction) {
        this.matchPrediction = matchPrediction;
    }
}
