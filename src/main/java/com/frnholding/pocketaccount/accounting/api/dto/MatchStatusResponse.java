package com.frnholding.pocketaccount.accounting.api.dto;

import java.math.BigDecimal;

public class MatchStatusResponse {
    private BigDecimal transactionAmountAbs;
    private BigDecimal sumMatched;
    private String status;
    
    public MatchStatusResponse() {
    }
    
    public MatchStatusResponse(BigDecimal transactionAmountAbs, BigDecimal sumMatched, String status) {
        this.transactionAmountAbs = transactionAmountAbs;
        this.sumMatched = sumMatched;
        this.status = status;
    }
    
    public BigDecimal getTransactionAmountAbs() {
        return transactionAmountAbs;
    }
    
    public void setTransactionAmountAbs(BigDecimal transactionAmountAbs) {
        this.transactionAmountAbs = transactionAmountAbs;
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
