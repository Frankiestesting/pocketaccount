package com.frnholding.pocketaccount.interpretation.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interpretation_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterpretationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(columnDefinition = "uuid", nullable = false)
    private UUID documentId;
    
    @Column(columnDefinition = "uuid", unique = true)
    private UUID jobId;
    
    @Column(nullable = false)
    private String documentType;
    
    @Column(nullable = false)
    private Instant interpretedAt;
    
    @Column(length = 500)
    private String extractionMethods;

    @Column(name = "account_no", length = 11)
    private String accountNo;
    
    @Embedded
    private InvoiceFieldsDTO invoiceFields;
    
    @OneToMany(mappedBy = "interpretationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatementTransaction> statementTransactions = new ArrayList<>();
}
