package com.frnholding.pocketaccount.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private String id;
    private String documentId;
    private String status;
    private Instant created;
    private String pipeline;
    private boolean useOcr;
    private boolean useAi;
    private String languageHint;
}