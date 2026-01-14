package com.frnholding.pocketaccount.interpretation.pipeline;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents text interpreted from a document.
 * Contains the raw text, structured data, and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterpretedText {
    private String rawText;
    private List<String> lines;
    private Map<String, Object> metadata;
    private boolean ocrUsed;
    private String languageDetected;
}
