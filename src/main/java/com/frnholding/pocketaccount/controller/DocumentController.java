package com.frnholding.pocketaccount.controller;

import com.frnholding.pocketaccount.api.dto.DocumentCorrectionRequest;
import com.frnholding.pocketaccount.api.dto.JobCreationRequest;
import com.frnholding.pocketaccount.api.dto.DocumentCorrectionResponse;
import com.frnholding.pocketaccount.api.dto.DocumentResponse;
import com.frnholding.pocketaccount.api.dto.DocumentUploadResponse;
import com.frnholding.pocketaccount.api.dto.ExtractionResultResponse;
import com.frnholding.pocketaccount.api.dto.JobCancelResponse;
import com.frnholding.pocketaccount.api.dto.JobCreationResponse;
import com.frnholding.pocketaccount.api.dto.JobStatusResponse;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.domain.Job;
import com.frnholding.pocketaccount.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/documents")
    public ResponseEntity<java.util.List<DocumentResponse>> getAllDocuments() {
        try {
            java.util.List<Document> documents = documentService.getAllDocuments();
            java.util.List<DocumentResponse> responses = documents.stream()
                    .map(doc -> new DocumentResponse(
                            doc.getId(),
                            doc.getStatus(),
                            doc.getDocumentType(),
                            doc.getCreated(),
                            doc.getOriginalFilename()
                    ))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("source") String source,
            @RequestParam("originalFilename") String originalFilename,
            @RequestParam(value = "documentType", defaultValue = "PDF") String documentType) {

        try {
            Document document = documentService.uploadDocument(file, source, originalFilename, documentType);
            DocumentUploadResponse response = new DocumentUploadResponse(
                    document.getId(),
                    document.getStatus(),
                    document.getCreated(),
                    document.getOriginalFilename(),
                    document.getDocumentType(),
                    file.getSize()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String documentId) {
        try {
            Document document = documentService.getDocument(documentId);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            DocumentResponse response = new DocumentResponse(
                    document.getId(),
                    document.getStatus(),
                    document.getDocumentType(),
                    document.getCreated(),
                    document.getOriginalFilename()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<Resource> getDocumentFile(@PathVariable String documentId) {
        try {
            Resource file = documentService.getDocumentFile(documentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documentId + ".pdf\"")
                    .body(file);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/documents/{documentId}/jobs")
    public ResponseEntity<JobCreationResponse> createJob(@PathVariable String documentId, @RequestBody JobCreationRequest request) {
        try {
            Job job = documentService.createJob(documentId, request.getPipeline(), request.isUseOcr(), request.isUseAi(), request.getLanguageHint());
            JobCreationResponse response = new JobCreationResponse(
                    job.getId(),
                    job.getDocumentId(),
                    job.getStatus(),
                    job.getCreated()
            );
            return ResponseEntity.accepted().body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        try {
            Job job = documentService.getJob(jobId);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }
            JobStatusResponse response = new JobStatusResponse(
                    job.getId(),
                    job.getDocumentId(),
                    job.getStatus(),
                    job.getStartedAt(),
                    job.getFinishedAt(),
                    job.getError()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<java.util.List<JobStatusResponse>> getAllJobs() {
        try {
            java.util.List<Job> jobs = documentService.getAllJobs();
            java.util.List<JobStatusResponse> responses = jobs.stream()
                    .map(job -> {
                        Document doc = documentService.getDocument(job.getDocumentId());
                        JobStatusResponse response = new JobStatusResponse(
                                job.getId(),
                                job.getDocumentId(),
                                job.getStatus(),
                                job.getStartedAt(),
                                job.getFinishedAt(),
                                job.getError()
                        );
                        if (doc != null) {
                            response.setDocumentType(doc.getDocumentType());
                            response.setOriginalFilename(doc.getOriginalFilename());
                        }
                        return response;
                    })
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<JobCancelResponse> cancelJob(@PathVariable String jobId) {
        try {
            Job job = documentService.cancelJob(jobId);
            JobCancelResponse response = new JobCancelResponse(
                    job.getId(),
                    job.getStatus(),
                    job.getFinishedAt()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/documents/{documentId}/result")
    public ResponseEntity<ExtractionResultResponse> getExtractionResult(@PathVariable String documentId) {
        try {
            ExtractionResultResponse result = documentService.getExtractionResult(documentId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/documents/{documentId}/correction")
    public ResponseEntity<DocumentCorrectionResponse> saveCorrection(
            @PathVariable String documentId,
            @RequestBody DocumentCorrectionRequest request) {
        try {
            DocumentCorrectionResponse response = documentService.saveCorrection(documentId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}