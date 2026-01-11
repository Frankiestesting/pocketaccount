package com.frnholding.pocketaccount.service;

import com.frnholding.pocketaccount.ExtractionResultResponse;
import com.frnholding.pocketaccount.DocumentCorrectionRequest;
import com.frnholding.pocketaccount.DocumentCorrectionResponse;
import com.frnholding.pocketaccount.domain.Correction;
import com.frnholding.pocketaccount.domain.CorrectionEntity;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.domain.DocumentEntity;
import com.frnholding.pocketaccount.domain.Job;
import com.frnholding.pocketaccount.domain.JobEntity;
import com.frnholding.pocketaccount.repository.CorrectionRepository;
import com.frnholding.pocketaccount.repository.DocumentRepository;
import com.frnholding.pocketaccount.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CorrectionRepository correctionRepository;

    public Document uploadDocument(MultipartFile file, String source, String originalFilename, String documentType) throws IOException {
        // Validate source
        if (!"mobile".equals(source) && !"web".equals(source)) {
            throw new IllegalArgumentException("Invalid source");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File must be a PDF");
        }

        // Generate ID
        String id = UUID.randomUUID().toString();

        // Save file
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filePath = UPLOAD_DIR + id + ".pdf";
        Path path = uploadPath.resolve(id + ".pdf");
        Files.copy(file.getInputStream(), path);

        // Create document
        Document document = new Document(id, "uploaded", Instant.now(), originalFilename, filePath, documentType);

        // Save to DB
        DocumentEntity entity = DocumentEntity.fromDomain(document);
        documentRepository.save(entity);

        return document;
    }

    public Document getDocument(String documentId) {
        DocumentEntity entity = documentRepository.findById(documentId).orElse(null);
        return entity != null ? entity.toDomain() : null;
    }

    public Resource getDocumentFile(String documentId) throws IOException {
        Document document = getDocument(documentId);
        if (document == null) {
            throw new FileNotFoundException("Document not found: " + documentId);
        }

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + document.getFilePath());
        }

        return new InputStreamResource(new FileInputStream(filePath.toFile()));
    }

    public Job createJob(String documentId, String pipeline, boolean useOcr, boolean useAi, String languageHint) {
        // Validate document exists
        Document document = getDocument(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        // Generate job ID
        String jobId = UUID.randomUUID().toString();

        // Create job
        Job job = new Job(jobId, documentId, "pending", Instant.now(), pipeline, useOcr, useAi, languageHint, null, null, null);

        // Save to DB
        JobEntity entity = JobEntity.fromDomain(job);
        jobRepository.save(entity);

        return job;
    }

    public Job getJob(String jobId) {
        JobEntity entity = jobRepository.findById(jobId).orElse(null);
        return entity != null ? entity.toDomain() : null;
    }

    public Job cancelJob(String jobId) {
        JobEntity entity = jobRepository.findById(jobId).orElse(null);
        if (entity == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        // Only allow cancellation of pending or running jobs
        String currentStatus = entity.getStatus();
        if (!"pending".equals(currentStatus) && !"running".equals(currentStatus)) {
            throw new IllegalStateException("Job cannot be cancelled. Current status: " + currentStatus);
        }

        entity.setStatus("cancelled");
        entity.setFinishedAt(Instant.now());
        jobRepository.save(entity);

        return entity.toDomain();
    }

    public ExtractionResultResponse getExtractionResult(String documentId) {
        // Validate document exists
        Document document = getDocument(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        String documentType = document.getDocumentType();
        
        if ("STATEMENT".equals(documentType)) {
            // Return STATEMENT structure with transactions
            List<ExtractionResultResponse.Transaction> transactions = List.of(
                new ExtractionResultResponse.Transaction(
                    "2026-01-03",
                    -399.00,
                    "NOK",
                    "KIWI 123",
                    Map.of("date", 0.95, "amount", 0.91)
                )
            );
            
            return new ExtractionResultResponse(
                documentId,
                "STATEMENT",
                1,
                null, // extractedAt not needed for STATEMENT
                null, // fields not used for STATEMENT
                null, // confidence not used for STATEMENT
                null, // warnings not used for STATEMENT
                transactions
            );
        } else {
            // Return INVOICE structure (existing logic)
            Map<String, Object> fields = Map.of(
                "date", "2026-01-02",
                "amount", 12450.00,
                "currency", "NOK",
                "sender", "Strøm AS",
                "description", "Faktura strøm januar"
            );

            Map<String, Double> confidence = Map.of(
                "date", 0.86,
                "amount", 0.93,
                "sender", 0.72,
                "description", 0.60
            );

            List<String> warnings = List.of("Sender confidence low");

            return new ExtractionResultResponse(
                documentId,
                "INVOICE",
                3,
                Instant.parse("2026-01-11T13:26:08Z"),
                fields,
                confidence,
                warnings,
                null // transactions not used for INVOICE
            );
        }
    }
}