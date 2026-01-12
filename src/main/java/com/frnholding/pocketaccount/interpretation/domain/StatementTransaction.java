package com.frnholding.pocketaccount.interpretation.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "statement_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interpretation_result_id", nullable = false)
    private InterpretationResult interpretationResult;
    
    private Double amount;
    private String currency;
    private LocalDate date;
    
    @Column(length = 1000)
    private String description;
}
