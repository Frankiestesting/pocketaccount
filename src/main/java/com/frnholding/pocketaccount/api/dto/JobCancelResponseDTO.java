package com.frnholding.pocketaccount.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCancelResponseDTO {
    private String jobId;
    private String status;
    private Instant cancelledAt;
}