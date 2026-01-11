package com.frnholding.pocketaccount;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCancelResponse {
    private String jobId;
    private String status;
    private Instant cancelledAt;
}