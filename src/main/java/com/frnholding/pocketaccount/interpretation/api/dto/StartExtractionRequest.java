package com.frnholding.pocketaccount.interpretation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting a document interpretation/extraction job.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartExtractionRequest {
    /**
     * Whether to use OCR for text extraction.
     * Set to true for scanned documents or images.
     */
    private boolean useOcr;
    
    /**
     * Whether to use AI-based extraction (OpenAI).
     * Set to true for higher accuracy at the cost of processing time and API costs.
     */
    private boolean useAi;
    
    /**
     * Language hint for text extraction and interpretation.
     * Examples: "en", "de", "fr", "eng+deu+fra"
     */
    private String languageHint;
    
    /**
     * Hinted document type to guide extraction.
     * Values: "INVOICE", "STATEMENT", "RECEIPT", "UNKNOWN"
     */
    private String hintedType;
}
