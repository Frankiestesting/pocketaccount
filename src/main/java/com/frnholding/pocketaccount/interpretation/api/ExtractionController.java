package com.frnholding.pocketaccount.interpretation.api;

import com.frnholding.pocketaccount.interpretation.api.dto.*;
import com.frnholding.pocketaccount.interpretation.service.InterpretationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for document interpretation and extraction operations.
 * Handles AI-powered and rule-based document interpretation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interpretation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ExtractionController {

    private final InterpretationService interpretationService;

    /**
     * Start a new interpretation/extraction job for a document.
     * POST /api/v1/interpretation/documents/{id}/jobs
     *
     * @param id Document ID to interpret
     * @param request Extraction configuration (OCR, AI, language hints)
     * @return Job creation response with job ID and status
     */
    @PostMapping("/documents/{id}/jobs")
    public ResponseEntity<StartExtractionResponse> startExtraction(
            @PathVariable String id,
            @RequestBody StartExtractionRequest request) {
        
        log.info("Starting extraction job for document {} with options: useOcr={}, useAi={}, languageHint={}, hintedType={}", 
                id, request.isUseOcr(), request.isUseAi(), request.getLanguageHint(), request.getHintedType());
        
        try {
            StartExtractionResponse response = interpretationService.startExtraction(id, request);
            return ResponseEntity.accepted().body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Document not found: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to start extraction for document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all interpretation jobs.
     * GET /api/v1/interpretation/jobs
     *
     * @return List of all interpretation jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<java.util.List<JobStatusResponse>> getAllJobs() {
        
        log.debug("Getting all interpretation jobs");
        
        try {
            java.util.List<JobStatusResponse> jobs = interpretationService.getAllJobs();
            return ResponseEntity.ok(jobs);
            
        } catch (Exception e) {
            log.error("Failed to get all jobs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get the status of an interpretation job.
     * GET /api/v1/interpretation/jobs/{jobId}
     *
     * @param jobId Job ID to query
     * @return Job status response with progress and results
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        
        log.debug("Getting status for interpretation job {}", jobId);
        
        try {
            JobStatusResponse response = interpretationService.getJobStatus(jobId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Job not found: {}", jobId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to get job status for {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get the interpretation/extraction results for a document.
     * GET /api/v1/interpretation/documents/{id}/result
     *
     * @param id Document ID to get results for
     * @return Extraction results (invoice fields or statement transactions)
     */
    @GetMapping("/documents/{id}/result")
    public ResponseEntity<ExtractionResultResponse> getExtractionResult(@PathVariable String id) {
        
        log.debug("Getting extraction result for document {}", id);
        
        try {
            ExtractionResultResponse response = interpretationService.getExtractionResult(id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Document or result not found: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to get extraction result for document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get the interpretation/extraction results for a specific job.
     * GET /api/v1/interpretation/jobs/{jobId}/result
     *
     * @param jobId Job ID to get results for
     * @return Extraction results (invoice fields or statement transactions)
     */
    @GetMapping("/jobs/{jobId}/result")
    public ResponseEntity<ExtractionResultResponse> getJobResult(@PathVariable String jobId) {
        
        log.debug("Getting extraction result for job {}", jobId);
        
        try {
            ExtractionResultResponse response = interpretationService.getJobResult(jobId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Job or result not found: {}", jobId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to get extraction result for job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Save corrections to interpretation results.
     * PUT /api/v1/interpretation/documents/{id}/correction
     *
     * @param id Document ID to correct
     * @param request Corrected fields and transactions
     * @return Confirmation of saved corrections
     */
    @PutMapping("/documents/{id}/correction")
    public ResponseEntity<Void> saveCorrection(
            @PathVariable String id,
            @RequestBody SaveCorrectionRequest request) {
        
        log.info("Saving correction for document {} of type {}", id, request.getDocumentType());
        
        try {
            interpretationService.saveCorrection(id, request);
            return ResponseEntity.ok().build();
            
        } catch (IllegalArgumentException e) {
            log.error("Document not found: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to save correction for document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
