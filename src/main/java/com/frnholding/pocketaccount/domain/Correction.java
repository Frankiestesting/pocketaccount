package com.frnholding.pocketaccount.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Correction {
    private Long id;
    private String documentId;
    private String documentType;
    private Map<String, Object> fields;
    private String note;
    private Integer correctionVersion;
    private Instant savedAt;
    private String savedBy;
    private Integer normalizedTransactionsCreated;
}