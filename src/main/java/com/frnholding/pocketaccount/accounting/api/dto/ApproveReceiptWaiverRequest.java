package com.frnholding.pocketaccount.accounting.api.dto;

import com.frnholding.pocketaccount.accounting.domain.ReceiptWaiverReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ApproveReceiptWaiverRequest {
    @NotNull(message = "reason must not be null")
    private ReceiptWaiverReason reason;

    @Size(max = 500, message = "note must be at most 500 characters")
    private String note;

    public ApproveReceiptWaiverRequest() {
    }

    public ApproveReceiptWaiverRequest(ReceiptWaiverReason reason, String note) {
        this.reason = reason;
        this.note = note;
    }

    public ReceiptWaiverReason getReason() {
        return reason;
    }

    public void setReason(ReceiptWaiverReason reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
