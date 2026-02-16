package com.frnholding.pocketaccount.interpretation.api;

import com.frnholding.pocketaccount.interpretation.api.dto.StartExtractionRequestDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.StartExtractionResponseDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.JobStatusResponseDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.ExtractionResultResponseDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.SaveCorrectionRequestDTO;
import com.frnholding.pocketaccount.interpretation.api.dto.ApproveStatementTransactionRequest;
import com.frnholding.pocketaccount.interpretation.api.dto.ApproveStatementTransactionResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptResponse;
import com.frnholding.pocketaccount.interpretation.api.dto.StatementTransactionResponseDTO;
import com.frnholding.pocketaccount.interpretation.infra.OpenAiConnectionService;
import com.frnholding.pocketaccount.interpretation.service.InterpretationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for document interpretation and extraction operations.
 * Handles AI-powered and rule-based document interpretation.
 */
@RestController
@RequestMapping("/api/v1/interpretation")
@CrossOrigin(origins = "*")
@Tag(name = "Interpretation", description = "Document interpretation and field extraction API")
public class ExtractionController {

    private static final Logger log = LoggerFactory.getLogger(ExtractionController.class);
    private final InterpretationService interpretationService;
        private final OpenAiConnectionService openAiConnectionService;

        public ExtractionController(InterpretationService interpretationService,
                                                                OpenAiConnectionService openAiConnectionService) {
        this.interpretationService = interpretationService;
                this.openAiConnectionService = openAiConnectionService;
    }

    @PostMapping("/documents/{id}/jobs")
    @Operation(summary = "Start document extraction", 
            description = "Create a new extraction job for a document using PDFBox, OCR, and/or AI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Extraction job started successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Server error starting extraction")
    })
    public ResponseEntity<StartExtractionResponseDTO> startExtraction(
            @PathVariable @Parameter(description = "Document ID") UUID id,
            @Valid @RequestBody @Parameter(description = "Extraction configuration") StartExtractionRequestDTO request) {
        
        log.info("Starting extraction job for document {} with options: useOcr={}, useAi={}, languageHint={}, hintedType={}", 
                id, request.isUseOcr(), request.isUseAi(), request.getLanguageHint(), request.getHintedType());

        StartExtractionResponseDTO response = interpretationService.startExtraction(id.toString(), request);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all extraction jobs", description = "Get all extraction jobs across all documents")
    @ApiResponse(responseCode = "200", description = "List of jobs retrieved successfully")
    public ResponseEntity<List<JobStatusResponseDTO>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        log.debug("Getting all interpretation jobs");

        List<JobStatusResponseDTO> jobs = interpretationService.getJobs(page, size);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status", description = "Get the current status of an extraction job (PENDING, COMPLETED, FAILED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<JobStatusResponseDTO> getJobStatus(@PathVariable @Parameter(description = "Job ID") UUID jobId) {
        
        log.debug("Getting status for interpretation job {}", jobId);

        JobStatusResponseDTO response = interpretationService.getJobStatus(jobId.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{id}/result")
    @Operation(summary = "Get extraction results by document", description = "Get extracted fields or transactions for a document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extraction results retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Document or results not found")
    })
    public ResponseEntity<ExtractionResultResponseDTO> getExtractionResult(@PathVariable @Parameter(description = "Document ID") UUID id) {
        
        log.debug("Getting extraction result for document {}", id);

        ExtractionResultResponseDTO response = interpretationService.getExtractionResult(id.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/{jobId}/result")
    @Operation(summary = "Get extraction results by job", description = "Get extracted fields or transactions for a specific job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extraction results retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Job or results not found")
    })
    public ResponseEntity<ExtractionResultResponseDTO> getJobResult(@PathVariable @Parameter(description = "Job ID") UUID jobId) {
        
        log.debug("Getting extraction result for job {}", jobId);

        ExtractionResultResponseDTO response = interpretationService.getJobResult(jobId.toString());
        return ResponseEntity.ok(response);
    }

        @PostMapping("/jobs/{jobId}/receipt")
        @Operation(summary = "Create receipt from interpretation result", description = "Create a receipt from a completed receipt interpretation job")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Receipt created"),
                        @ApiResponse(responseCode = "400", description = "Missing receipt fields"),
                        @ApiResponse(responseCode = "404", description = "Job not found"),
                        @ApiResponse(responseCode = "409", description = "Receipt already exists")
        })
        public ResponseEntity<ReceiptResponse> createReceiptFromJob(
                        @PathVariable @Parameter(description = "Job ID") UUID jobId) {
                ReceiptResponse receipt = interpretationService.createReceiptFromJob(jobId.toString());
                return ResponseEntity.status(201).body(receipt);
        }

        @PostMapping("/documents/{documentId}/receipt")
        @Operation(summary = "Create receipt from document interpretation", description = "Create a receipt from the latest receipt interpretation for a document")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Receipt created"),
                        @ApiResponse(responseCode = "400", description = "Missing receipt fields"),
                        @ApiResponse(responseCode = "404", description = "Document not found"),
                        @ApiResponse(responseCode = "409", description = "Receipt already exists")
        })
        public ResponseEntity<ReceiptResponse> createReceiptFromDocument(
                        @PathVariable @Parameter(description = "Document ID") UUID documentId) {
                ReceiptResponse receipt = interpretationService.createReceiptFromDocument(documentId.toString());
                return ResponseEntity.status(201).body(receipt);
        }

        @GetMapping("/jobs/{jobId}/statement-transactions")
        @Operation(summary = "List statement transactions by job", description = "Get statement transactions for a completed interpretation job")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Statement transactions retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Job not found")
        })
        public ResponseEntity<List<StatementTransactionResponseDTO>> getStatementTransactionsForJob(
                        @PathVariable @Parameter(description = "Job ID") UUID jobId) {

                List<StatementTransactionResponseDTO> transactions =
                                interpretationService.getStatementTransactionsForJob(jobId.toString());
                return ResponseEntity.ok(transactions);
        }

    @PutMapping("/documents/{id}/correction")
    @Operation(summary = "Save extraction corrections", description = "Save user corrections to extracted fields or transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Corrections saved successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Server error saving corrections")
    })
    public ResponseEntity<Void> saveCorrection(
            @PathVariable @Parameter(description = "Document ID") UUID id,
            @Valid @RequestBody @Parameter(description = "Corrected extraction data") SaveCorrectionRequestDTO request) {
        
        log.info("Saving correction for document {} of type {}", id, request.getDocumentType());

                interpretationService.saveCorrection(id.toString(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/statement-transactions/{id}/approve")
    @Operation(summary = "Approve statement transaction", description = "Approve a statement transaction and ensure a linked bank transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statement transaction approved"),
            @ApiResponse(responseCode = "404", description = "Statement transaction or account not found"),
            @ApiResponse(responseCode = "400", description = "Missing required transaction fields")
    })
    public ResponseEntity<ApproveStatementTransactionResponse> approveStatementTransaction(
            @PathVariable @Parameter(description = "Statement transaction ID") Long id,
            @RequestBody(required = false) ApproveStatementTransactionRequest request) {
        log.info("Approving statement transaction {}", id);
        var bankTransaction = interpretationService.approveStatementTransaction(
                id,
                request != null ? request.getAccountId() : null
        );
        ApproveStatementTransactionResponse response = new ApproveStatementTransactionResponse(
                id,
                bankTransaction.getId(),
                true
        );
        return ResponseEntity.ok(response);
    }

        @GetMapping("/statement-transactions/{id}")
        @Operation(summary = "Get statement transaction", description = "Fetch a single statement transaction")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Statement transaction retrieved"),
                        @ApiResponse(responseCode = "404", description = "Statement transaction not found")
        })
        public ResponseEntity<StatementTransactionResponseDTO> getStatementTransactionById(
                        @PathVariable @Parameter(description = "Statement transaction ID") Long id) {
                StatementTransactionResponseDTO transaction = interpretationService.getStatementTransactionById(id);
                return ResponseEntity.ok(transaction);
        }

        @GetMapping("/openai/check")
        @Operation(summary = "Check OpenAI connection", description = "Checks OpenAI API connectivity and returns the raw OpenAI response or error.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "OpenAI connection check succeeded"),
                        @ApiResponse(responseCode = "400", description = "OpenAI not enabled or API key missing"),
                        @ApiResponse(responseCode = "401", description = "OpenAI authentication failed"),
                        @ApiResponse(responseCode = "500", description = "OpenAI check failed")
        })
        public ResponseEntity<String> checkOpenAiConnection() {
                log.info("Checking OpenAI connection");
                return openAiConnectionService.checkConnection();
        }
}
