package com.frnholding.pocketaccount.interpretation.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Options for configuring the interpretation pipeline
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterpretationOptions {
    private boolean useOcr;
    private boolean useAi;
    private String languageHint;
    private DocumentType hintedType;
}
