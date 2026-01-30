package com.frnholding.pocketaccount.accounting.api.dto;

public class CreateAccountRequestDTO {
    
    private String name;
    private String currency;
    
    public CreateAccountRequestDTO() {
    }
    
    public CreateAccountRequestDTO(String name, String currency) {
        this.name = name;
        this.currency = currency;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
