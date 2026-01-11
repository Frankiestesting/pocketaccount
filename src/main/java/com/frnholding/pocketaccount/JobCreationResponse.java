package com.frnholding.pocketaccount;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCreationResponse {
    private String jobId;
    private String documentId;
    private String status;
    private Instant created;
}