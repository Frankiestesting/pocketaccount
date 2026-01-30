package com.frnholding.pocketaccount.interpretation.api.dto;

/**
 * Request DTO for starting a document interpretation/extraction job.
 */
public class StartExtractionRequestDTO {
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

    public StartExtractionRequestDTO() {
    }

    public StartExtractionRequestDTO(boolean useOcr, boolean useAi, String languageHint, String hintedType) {
        this.useOcr = useOcr;
        this.useAi = useAi;
        this.languageHint = languageHint;
        this.hintedType = hintedType;
    }

    public boolean isUseOcr() {
        return useOcr;
    }

    public void setUseOcr(boolean useOcr) {
        this.useOcr = useOcr;
    }

    public boolean isUseAi() {
        return useAi;
    }

    public void setUseAi(boolean useAi) {
        this.useAi = useAi;
    }

    public String getLanguageHint() {
        return languageHint;
    }

    public void setLanguageHint(String languageHint) {
        this.languageHint = languageHint;
    }

    public String getHintedType() {
        return hintedType;
    }

    public void setHintedType(String hintedType) {
        this.hintedType = hintedType;
    }
}
