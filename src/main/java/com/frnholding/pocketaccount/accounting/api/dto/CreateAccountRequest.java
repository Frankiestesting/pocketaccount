package com.frnholding.pocketaccount.accounting.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class CreateAccountRequest {
    
    @NotBlank(message = "Account name is required")
    @Size(max = 200, message = "Account name must not exceed 200 characters")
    private String name;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO code (e.g., USD, EUR, NOK)")
    private String currency;

    @NotNull(message = "Account number is required")
    @Min(value = 10000000000L, message = "Account number must be 11 digits")
    @Max(value = 99999999999L, message = "Account number must be 11 digits")
    private Long accountNo;
    
    public CreateAccountRequest() {
    }
    
    public CreateAccountRequest(String name, String currency, Long accountNo) {
        this.name = name;
        this.currency = currency;
        this.accountNo = accountNo;
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

    public Long getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(Long accountNo) {
        this.accountNo = accountNo;
    }
}
