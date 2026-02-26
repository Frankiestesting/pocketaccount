package com.frnholding.pocketaccount.controller;

import com.frnholding.pocketaccount.api.dto.DocumentCorrectionRequestDTO;
import com.frnholding.pocketaccount.api.dto.JobCreationRequestDTO;
import com.frnholding.pocketaccount.api.dto.DocumentCorrectionResponseDTO;
import com.frnholding.pocketaccount.api.dto.DocumentResponseDTO;
import com.frnholding.pocketaccount.api.dto.DocumentUploadResponseDTO;
import com.frnholding.pocketaccount.api.dto.ExtractionResultResponseDTO;
import com.frnholding.pocketaccount.api.dto.JobCancelResponseDTO;
import com.frnholding.pocketaccount.api.dto.JobCreationResponseDTO;
import com.frnholding.pocketaccount.api.dto.JobStatusResponseDTO;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.domain.Job;
import com.frnholding.pocketaccount.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@Tag(name = "Documents", description = "Document upload and management API")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/documents")
    @Operation(summary = "Get all documents", description = "Retrieve a list of all uploaded documents")
    @ApiResponse(responseCode = "200", description = "List of documents retrieved successfully")
    public ResponseEntity<List<DocumentResponseDTO>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<Document> documents = documentService.getDocuments(page, size);
        List<DocumentResponseDTO> responses = documents.stream()
                .map(doc -> new DocumentResponseDTO(
                        doc.getId(),
                        doc.getStatus(),
                        doc.getDocumentType(),
                        doc.getCreated(),
                        doc.getOriginalFilename()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Upload a new document", description = "Upload a PDF or image document for processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
            @ApiResponse(responseCode = "500", description = "Server error during upload")
    })
    public ResponseEntity<DocumentUploadResponseDTO> uploadDocument(
            @RequestParam("file") @Parameter(description = "PDF/PNG/JPG file to upload") MultipartFile file,
            @RequestParam("source") @Parameter(description = "Document source/origin") String source,
            @RequestParam("originalFilename") @Parameter(description = "Original filename") String originalFilename,
            @RequestParam(value = "documentType", defaultValue = "PDF") @Parameter(description = "Document type (INVOICE, STATEMENT, RECEIPT, PDF)") String documentType) throws IOException {
        Document document = documentService.uploadDocument(file, source, originalFilename, documentType);
        DocumentUploadResponseDTO response = new DocumentUploadResponseDTO(
                document.getId(),
                document.getStatus(),
                document.getCreated(),
                document.getOriginalFilename(),
                document.getDocumentType(),
                file.getSize()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "Get document metadata", description = "Retrieve metadata for a specific document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document metadata retrieved"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable @Parameter(description = "Document ID") UUID documentId) {
        Document document = documentService.getDocument(documentId);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        DocumentResponseDTO response = new DocumentResponseDTO(
                document.getId(),
                document.getStatus(),
                document.getDocumentType(),
                document.getCreated(),
                document.getOriginalFilename()
        );
        return ResponseEntity.ok(response);
    }

        @DeleteMapping("/documents/{documentId}")
        @Operation(summary = "Delete document", description = "Delete a document and its interpretation jobs/results")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Document not found")
        })
        public ResponseEntity<Void> deleteDocument(@PathVariable @Parameter(description = "Document ID") UUID documentId) {
                documentService.deleteDocument(documentId);
                return ResponseEntity.noContent().build();
        }

    @GetMapping("/documents/{documentId}/file")
    @Operation(summary = "Download document file", description = "Download the file for a document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File retrieved successfully", 
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<Resource> getDocumentFile(@PathVariable @Parameter(description = "Document ID") UUID documentId) throws IOException {
        Document document = documentService.getDocument(documentId);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        Resource file = documentService.getDocumentFile(documentId);
        MediaType mediaType = resolveMediaType(document.getFilePath(), document.getOriginalFilename());
        String downloadName = resolveDownloadName(document, documentId.toString());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadName + "\"")
                .body(file);
    }

    private MediaType resolveMediaType(String filePath, String originalFilename) {
        String extension = getExtension(filePath != null ? filePath : originalFilename);
        if ("png".equals(extension)) {
            return MediaType.IMAGE_PNG;
        }
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_PDF;
    }

    private String resolveDownloadName(Document document, String documentId) {
        String originalFilename = document.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            return originalFilename;
        }
        String extension = getExtension(document.getFilePath());
        if (extension == null) {
            return documentId;
        }
        return documentId + "." + extension;
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    @PostMapping("/documents/{documentId}/jobs")
    @Operation(summary = "Create extraction job", description = "Create a new extraction/interpretation job for a document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Job created successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<JobCreationResponseDTO> createJob(
            @PathVariable @Parameter(description = "Document ID") UUID documentId,
            @Valid @RequestBody @Parameter(description = "Job configuration") JobCreationRequestDTO request) {
        Job job = documentService.createJob(
            documentId,
                request.getPipeline(),
                request.isUseOcr(),
                request.isUseAi(),
                request.getLanguageHint()
        );
        JobCreationResponseDTO response = new JobCreationResponseDTO(
                job.getId(),
                job.getDocumentId(),
                job.getStatus(),
                job.getCreated()
        );
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status", description = "Get the current status of an extraction job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<JobStatusResponseDTO> getJobStatus(@PathVariable @Parameter(description = "Job ID") UUID jobId) {
        Job job = documentService.getJob(jobId.toString());
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        JobStatusResponseDTO response = new JobStatusResponseDTO(
                job.getId(),
                job.getDocumentId(),
                job.getStatus(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getError()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all jobs", description = "Get all extraction jobs across all documents")
    @ApiResponse(responseCode = "200", description = "List of jobs retrieved successfully")
    public ResponseEntity<List<JobStatusResponseDTO>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<Job> jobs = documentService.getJobs(page, size);
        List<UUID> documentIds = jobs.stream()
                .map(Job::getDocumentId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, Document> documentsById = documentService.getDocumentsByIds(documentIds);

        List<JobStatusResponseDTO> responses = jobs.stream()
                .map(job -> {
                    Document doc = documentsById.get(job.getDocumentId());
                    JobStatusResponseDTO response = new JobStatusResponseDTO(
                            job.getId(),
                            job.getDocumentId(),
                            job.getStatus(),
                            job.getStartedAt(),
                            job.getFinishedAt(),
                            job.getError()
                    );
                    response.setPipeline(job.getPipeline());
                    response.setUseAi(job.isUseAi());
                    if (doc != null) {
                        response.setDocumentType(doc.getDocumentType());
                        response.setOriginalFilename(doc.getOriginalFilename());
                    }
                    return response;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @Operation(summary = "Cancel job", description = "Cancel a running or pending extraction job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "400", description = "Job cannot be cancelled in current state")
    })
    public ResponseEntity<JobCancelResponseDTO> cancelJob(@PathVariable @Parameter(description = "Job ID") UUID jobId) {
        Job job = documentService.cancelJob(jobId.toString());
        JobCancelResponseDTO response = new JobCancelResponseDTO(
                job.getId(),
                job.getStatus(),
                job.getFinishedAt()
        );
        return ResponseEntity.ok(response);
    }

        @DeleteMapping("/jobs/{jobId}")
        @Operation(summary = "Delete job", description = "Delete a job without deleting the underlying document file")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Job deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Job not found")
        })
        public ResponseEntity<Void> deleteJob(@PathVariable @Parameter(description = "Job ID") UUID jobId) {
                documentService.deleteJob(jobId.toString());
                return ResponseEntity.noContent().build();
        }

    @GetMapping("/documents/{documentId}/result")
    @Operation(summary = "Get extraction result", description = "Get extracted fields or transactions for a document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Extraction result retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Document or result not found")
    })
    public ResponseEntity<ExtractionResultResponseDTO> getExtractionResult(@PathVariable @Parameter(description = "Document ID") UUID documentId) {
        ExtractionResultResponseDTO result = documentService.getExtractionResult(documentId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/documents/{documentId}/correction")
    @Operation(summary = "Save extraction corrections", description = "Save user corrections to extracted fields or transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Corrections saved successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentCorrectionResponseDTO> saveCorrection(
            @PathVariable @Parameter(description = "Document ID") UUID documentId,
            @Valid @RequestBody @Parameter(description = "Corrected extraction data") DocumentCorrectionRequestDTO request) {
        DocumentCorrectionResponseDTO response = documentService.saveCorrection(documentId, request);
        return ResponseEntity.ok(response);
    }
}
