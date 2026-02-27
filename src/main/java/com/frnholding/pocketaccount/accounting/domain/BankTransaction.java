package com.frnholding.pocketaccount.accounting.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_transaction", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "source_line_hash"}))
public class BankTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;
    
    @Column(name = "value_date")
    private LocalDate valueDate;
    
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(length = 200)
    private String counterparty;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 200)
    private String reference;
    
    @Column(name = "source_document_id")
    private UUID sourceDocumentId;
    
    @Column(name = "source_line_hash", nullable = false, length = 64)
    private String sourceLineHash;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "receipt_waived", nullable = false)
    private boolean receiptWaived;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_waiver_reason", length = 50)
    private ReceiptWaiverReason receiptWaiverReason;

    @Column(name = "receipt_waiver_note", columnDefinition = "TEXT")
    private String receiptWaiverNote;

    @Column(name = "receipt_waived_at")
    private OffsetDateTime receiptWaivedAt;
    
    public BankTransaction() {
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Account getAccount() {
        return account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
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
