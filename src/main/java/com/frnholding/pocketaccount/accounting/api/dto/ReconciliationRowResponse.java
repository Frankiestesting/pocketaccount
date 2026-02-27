package com.frnholding.pocketaccount.accounting.api.dto;

import com.frnholding.pocketaccount.accounting.domain.ReceiptWaiverReason;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ReconciliationRowResponse {
    private UUID transactionId;
    private LocalDate bookingDate;
    private BigDecimal amount;
    private String description;
    private BigDecimal sumMatched;
    private String status;
    private boolean receiptWaived;
    private ReceiptWaiverReason receiptWaiverReason;
    private String receiptWaiverNote;
    private OffsetDateTime receiptWaivedAt;
    
    public ReconciliationRowResponse() {
    }
    
    public ReconciliationRowResponse(UUID transactionId, LocalDate bookingDate, BigDecimal amount,
                                    String description, BigDecimal sumMatched, String status,
                                    boolean receiptWaived, ReceiptWaiverReason receiptWaiverReason,
                                    String receiptWaiverNote, OffsetDateTime receiptWaivedAt) {
        this.transactionId = transactionId;
        this.bookingDate = bookingDate;
        this.amount = amount;
        this.description = description;
        this.sumMatched = sumMatched;
        this.status = status;
        this.receiptWaived = receiptWaived;
        this.receiptWaiverReason = receiptWaiverReason;
        this.receiptWaiverNote = receiptWaiverNote;
        this.receiptWaivedAt = receiptWaivedAt;
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
