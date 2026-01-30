package com.frnholding.pocketaccount.interpretation.domain;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class InvoiceFieldsDTO {
    private Double amount;
    private String currency;
    private LocalDate date;
    private String description;
    private String sender;

    public InvoiceFieldsDTO() {
    }

    public InvoiceFieldsDTO(Double amount, String currency, LocalDate date, String description, String sender) {
        this.amount = amount;
        this.currency = currency;
        this.date = date;
        this.description = description;
        this.sender = sender;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
