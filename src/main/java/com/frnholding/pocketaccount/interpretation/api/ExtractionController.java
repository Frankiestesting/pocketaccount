package com.frnholding.pocketaccount.interpretation.api;

import com.frnholding.pocketaccount.interpretation.api.dto.StartExtractionRequestDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.StartExtractionResponseDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.JobStatusResponseDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.ExtractionResultResponseDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.SaveCorrectionRequestDTO;
import com.frnholding.pocketaccount.interpretation.service.InterpretationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Interpretation", description = "Document interpretation and field extraction API")
public class ExtractionController {

    private final InterpretationService interpretationService;

    @PostMapping("/documents/{id}/jobs")
    @Operation(summary = "Start document extraction", 
            description = "Create a new extraction job for a document using PDFBox, OCR, and/or AI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Extraction job started successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Server error starting extraction")
    })
    public ResponseEntity<StartExtractionResponseDTO> startExtraction(
            @PathVariable @Parameter(description = "Document ID") String id,
            @RequestBody @Parameter(description = "Extraction configuration") StartExtractionRequestDTO request) {
        
        log.info("Starting extraction job for document {} with options: useOcr={}, useAi={}, languageHint={}, hintedType={}", 
                id, request.isUseOcr(), request.isUseAi(), request.getLanguageHint(), request.getHintedType());
        
        try {
            StartExtractionResponseDTO response = interpretationService.startExtraction(id, request);
            return ResponseEntity.accepted().body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Document not found: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to start extraction for document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all extraction jobs", description = "Get all extraction jobs across all documents")
    @ApiResponse(responseCode = "200", description = "List of jobs retrieved successfully")
    public ResponseEntity<java.util.List<JobStatusResponseDTO>> getAllJobs() {
        
        log.debug("Getting all interpretation jobs");
        
        try {
            java.util.List<JobStatusResponseDTO> jobs = interpretationService.getAllJobs();
            return ResponseEntity.ok(jobs);
            
        } catch (Exception e) {
            log.error("Failed to get all jobs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status", description = "Get the current status of an extraction job (PENDING, COMPLETED, FAILED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<JobStatusResponseDTO> getJobStatus(@PathVariable @Parameter(description = "Job ID") String jobId) {
        
        log.debug("Getting status for interpretation job {}", jobId);
        
        try {
            JobStatusResponseDTO response = interpretationService.getJobStatus(jobId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Job not found: {}", jobId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to get job status for {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/documents/{id}/result")
    @Operation(summary = "Get extraction results by document", description = "Get extracted fields or transactions for a document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extraction results retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Document or results not found")
    })
    public ResponseEntity<ExtractionResultResponseDTO> getExtractionResult(@PathVariable @Parameter(description = "Document ID") String id) {
        
        log.debug("Getting extraction result for document {}", id);
        
        try {
            ExtractionResultResponseDTO response = interpretationService.getExtractionResult(id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Document or result not found: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to get extraction result for document {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/jobs/{jobId}/result")
    @Operation(summary = "Get extraction results by job", description = "Get extracted fields or transactions for a specific job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extraction results retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Job or results not found")
    })
    public ResponseEntity<ExtractionResultResponseDTO> getJobResult(@PathVariable @Parameter(description = "Job ID") String jobId) {
        
        log.debug("Getting extraction result for job {}", jobId);
        
        try {
            ExtractionResultResponseDTO response = interpretationService.getJobResult(jobId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Job or result not found: {}", jobId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Failed to get extraction result for job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/documents/{id}/correction")
    @Operation(summary = "Save extraction corrections", description = "Save user corrections to extracted fields or transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Corrections saved successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Server error saving corrections")
    })
    public ResponseEntity<Void> saveCorrection(
            @PathVariable @Parameter(description = "Document ID") String id,
            @RequestBody @Parameter(description = "Corrected extraction data") SaveCorrectionRequestDTO request) {
        
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
