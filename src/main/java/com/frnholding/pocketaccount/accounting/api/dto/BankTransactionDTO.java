package com.frnholding.pocketaccount.accounting.api.dto;

import java.math.BigDecimal;
import com.frnholding.pocketaccount.accounting.domain.ReceiptWaiverReason;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class BankTransactionDTO {
    
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
    private boolean receiptWaived;
    private ReceiptWaiverReason receiptWaiverReason;
    private String receiptWaiverNote;
    private OffsetDateTime receiptWaivedAt;
    
    public BankTransactionDTO() {
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

    public boolean isReceiptWaived() {
        return receiptWaived;
    }

    public void setReceiptWaived(boolean receiptWaived) {
        this.receiptWaived = receiptWaived;
    }

    public ReceiptWaiverReason getReceiptWaiverReason() {
        return receiptWaiverReason;
    }

    public void setReceiptWaiverReason(ReceiptWaiverReason receiptWaiverReason) {
        this.receiptWaiverReason = receiptWaiverReason;
    }

    public String getReceiptWaiverNote() {
        return receiptWaiverNote;
    }

    public void setReceiptWaiverNote(String receiptWaiverNote) {
        this.receiptWaiverNote = receiptWaiverNote;
    }

    public OffsetDateTime getReceiptWaivedAt() {
        return receiptWaivedAt;
    }

    public void setReceiptWaivedAt(OffsetDateTime receiptWaivedAt) {
        this.receiptWaivedAt = receiptWaivedAt;
    }
}
