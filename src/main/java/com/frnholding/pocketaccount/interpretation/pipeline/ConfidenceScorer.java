package com.frnholding.pocketaccount.interpretation.pipeline;

import java.util.Map;

/**
 * Calculates confidence scores for extracted fields.
 * Scores range from 0.0 (no confidence) to 1.0 (high confidence).
 */
public interface ConfidenceScorer {
    
    /**
     * Calculates confidence scores for extracted fields
     * @param extractedFields map of field names to extracted values
     * @param interpretedText the original interpreted text
     * @return map of field names to confidence scores (0.0 to 1.0)
     */
    Map<String, Double> score(Map<String, Object> extractedFields, InterpretedText interpretedText);
}
