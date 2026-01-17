package com.frnholding.pocketaccount.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCreationRequestDTO {
    private String pipeline;
    private boolean useOcr;
    private boolean useAi;
    private String languageHint;
}