package com.frnholding.pocketaccount.accounting.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt_waiver_reason")
public class ReceiptWaiverReasonEntity {

    @Id
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ReceiptWaiverReasonEntity() {
    }

    public ReceiptWaiverReasonEntity(String code, String label, int sortOrder) {
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
