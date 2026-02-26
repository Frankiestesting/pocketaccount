package com.frnholding.pocketaccount.interpretation.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interpretation_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterpretationJob {
    @Id
    private String id;
    
    @Column(columnDefinition = "uuid", nullable = false)
    private UUID documentId;
    
    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private Instant created;
    
    private Instant startedAt;
    
    private Instant finishedAt;
    
    private String error;
    
    @Column(nullable = false)
    private String documentType;
}
