package com.frnholding.pocketaccount.service;

import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.domain.DocumentEntity;
import com.frnholding.pocketaccount.domain.Job;
import com.frnholding.pocketaccount.domain.JobEntity;
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
import java.util.UUID;

@Service
public class DocumentService {

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JobRepository jobRepository;

    public Document uploadDocument(MultipartFile file, String source, String originalFilename) throws IOException {
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
        Document document = new Document(id, "uploaded", Instant.now(), originalFilename, filePath, "PDF");

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
}