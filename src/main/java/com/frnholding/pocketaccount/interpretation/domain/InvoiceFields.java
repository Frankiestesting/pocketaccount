package com.frnholding.pocketaccount.interpretation.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFields {
    private Double amount;
    private String currency;
    private LocalDate date;
    private String description;
    private String sender;
}
