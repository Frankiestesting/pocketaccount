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
    public ResponseEntity<java.util.List<DocumentResponseDTO>> getAllDocuments() {
        try {
            java.util.List<Document> documents = documentService.getAllDocuments();
            java.util.List<DocumentResponseDTO> responses = documents.stream()
                    .map(doc -> new DocumentResponseDTO(
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
    public ResponseEntity<DocumentUploadResponseDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("source") String source,
            @RequestParam("originalFilename") String originalFilename,
            @RequestParam(value = "documentType", defaultValue = "PDF") String documentType) {

        try {
            Document document = documentService.uploadDocument(file, source, originalFilename, documentType);
            DocumentUploadResponseDTO response = new DocumentUploadResponseDTO(
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
    public ResponseEntity<DocumentResponseDTO> getDocument(@PathVariable String documentId) {
        try {
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
    public ResponseEntity<JobCreationResponseDTO> createJob(@PathVariable String documentId, @RequestBody JobCreationRequestDTO request) {
        try {
            Job job = documentService.createJob(documentId, request.getPipeline(), request.isUseOcr(), request.isUseAi(), request.getLanguageHint());
            JobCreationResponseDTO response = new JobCreationResponseDTO(
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
    public ResponseEntity<JobStatusResponseDTO> getJobStatus(@PathVariable String jobId) {
        try {
            Job job = documentService.getJob(jobId);
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
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<java.util.List<JobStatusResponseDTO>> getAllJobs() {
        try {
            java.util.List<Job> jobs = documentService.getAllJobs();
            java.util.List<JobStatusResponseDTO> responses = jobs.stream()
                    .map(job -> {
                        Document doc = documentService.getDocument(job.getDocumentId());
                        JobStatusResponseDTO response = new JobStatusResponseDTO(
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
    public ResponseEntity<JobCancelResponseDTO> cancelJob(@PathVariable String jobId) {
        try {
            Job job = documentService.cancelJob(jobId);
            JobCancelResponseDTO response = new JobCancelResponseDTO(
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
    public ResponseEntity<ExtractionResultResponseDTO> getExtractionResult(@PathVariable String documentId) {
        try {
            ExtractionResultResponseDTO result = documentService.getExtractionResult(documentId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/documents/{documentId}/correction")
    public ResponseEntity<DocumentCorrectionResponseDTO> saveCorrection(
            @PathVariable String documentId,
            @RequestBody DocumentCorrectionRequestDTO request) {
        try {
            DocumentCorrectionResponseDTO response = documentService.saveCorrection(documentId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}