package com.frnholding.pocketaccount.interpretation.api.dto;

import java.time.LocalDate;

public class StatementTransactionResponseDTO {
    private Long id;
    private LocalDate date;
    private String description;
    private String currency;
    private Double amount;
    private String accountNo;
    private Boolean approved;

    public StatementTransactionResponseDTO() {
    }

    public StatementTransactionResponseDTO(Long id, LocalDate date, String description,
                                           String currency, Double amount, String accountNo,
                                           Boolean approved) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.currency = currency;
        this.amount = amount;
        this.accountNo = accountNo;
        this.approved = approved;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}
