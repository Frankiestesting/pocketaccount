package com.frnholding.pocketaccount.interpretation.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterpreterJob {
    private String id;
    private String documentId;
    private String status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private String error;
}
