package com.frnholding.pocketaccount.accounting.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receipt_match",
       uniqueConstraints = @UniqueConstraint(columnNames = {"receipt_id", "bank_transaction_id"}))
public class ReceiptMatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id", nullable = false)
    private BankTransaction bankTransaction;
    
    @Column(name = "matched_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal matchedAmount;
    
    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType;
    
    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    public ReceiptMatch() {
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Receipt getReceipt() {
        return receipt;
    }
    
    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }
    
    public BankTransaction getBankTransaction() {
        return bankTransaction;
    }
    
    public void setBankTransaction(BankTransaction bankTransaction) {
        this.bankTransaction = bankTransaction;
    }
    
    public BigDecimal getMatchedAmount() {
        return matchedAmount;
    }
    
    public void setMatchedAmount(BigDecimal matchedAmount) {
        this.matchedAmount = matchedAmount;
    }
    
    public String getMatchType() {
        return matchType;
    }
    
    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }
    
    public BigDecimal getConfidence() {
        return confidence;
    }
    
    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
