package com.frnholding.pocketaccount;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCorrectionResponse {
    private String documentId;
    private Integer correctionVersion;
    private Instant savedAt;
    private String savedBy;
    private Integer normalizedTransactionsCreated;
}