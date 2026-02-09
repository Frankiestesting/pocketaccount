package com.frnholding.pocketaccount.service;

import com.frnholding.pocketaccount.api.dto.ExtractionResultResponseDTO;
import com.frnholding.pocketaccount.api.dto.DocumentCorrectionRequestDTO;
import com.frnholding.pocketaccount.api.dto.DocumentCorrectionResponseDTO;
import com.frnholding.pocketaccount.domain.Correction;
import com.frnholding.pocketaccount.domain.CorrectionEntity;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.domain.DocumentEntity;
import com.frnholding.pocketaccount.domain.Job;
import com.frnholding.pocketaccount.domain.JobEntity;
import com.frnholding.pocketaccount.exception.EntityNotFoundException;
import com.frnholding.pocketaccount.repository.CorrectionRepository;
import com.frnholding.pocketaccount.repository.DocumentRepository;
import com.frnholding.pocketaccount.repository.JobRepository;
import com.frnholding.pocketaccount.interpretation.domain.InterpretationResult;
import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;
import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CorrectionRepository correctionRepository;

    @Autowired
    private InterpretationResultRepository interpretationResultRepository;

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

    public List<Document> getAllDocuments() {
        List<DocumentEntity> entities = documentRepository.findAll();
        return entities.stream()
                .map(DocumentEntity::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Document> getDocuments(int page, int size) {
        return documentRepository.findAll(PageRequest.of(page, size)).stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toList());
    }

    public Map<String, Document> getDocumentsByIds(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return documentRepository.findAllById(documentIds).stream()
                .map(DocumentEntity::toDomain)
                .collect(Collectors.toMap(Document::getId, document -> document));
    }

    public Resource getDocumentFile(String documentId) throws IOException {
        Document document = getDocument(documentId);
        if (document == null) {
            throw new EntityNotFoundException("Document not found: " + documentId);
        }

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new EntityNotFoundException("File not found: " + document.getFilePath());
        }

        return new InputStreamResource(new FileInputStream(filePath.toFile()));
    }

    public Job createJob(String documentId, String pipeline, boolean useOcr, boolean useAi, String languageHint) {
        // Validate document exists
        Document document = getDocument(documentId);
        if (document == null) {
            throw new EntityNotFoundException("Document not found: " + documentId);
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

    public List<Job> getAllJobs() {
        List<JobEntity> entities = jobRepository.findAll();
        return entities.stream()
                .map(JobEntity::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Job> getJobs(int page, int size) {
        return jobRepository.findAll(PageRequest.of(page, size)).stream()
                .map(JobEntity::toDomain)
                .collect(Collectors.toList());
    }

    public Job cancelJob(String jobId) {
        JobEntity entity = jobRepository.findById(jobId).orElse(null);
        if (entity == null) {
            throw new EntityNotFoundException("Job not found: " + jobId);
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

    public ExtractionResultResponseDTO getExtractionResult(String documentId) {
        // Validate document exists
        Document document = getDocument(documentId);
        if (document == null) {
            throw new EntityNotFoundException("Document not found: " + documentId);
        }

        CorrectionEntity latestCorrection = correctionRepository
            .findTopByDocumentIdOrderByCorrectionVersionDesc(documentId)
            .orElse(null);

        Map<String, Object> correctedFields = latestCorrection != null
            ? latestCorrection.getFields()
            : null;
        String correctedDocumentType = latestCorrection != null && latestCorrection.getDocumentType() != null
            ? latestCorrection.getDocumentType()
            : document.getDocumentType();

        InterpretationResult interpretationResult = interpretationResultRepository
            .findByDocumentId(documentId)
            .orElse(null);

        Map<String, Object> extractedFields = null;
        List<ExtractionResultResponseDTO.Transaction> extractedTransactions = null;
        Instant extractedAt = null;

        if (interpretationResult != null) {
            extractedAt = interpretationResult.getInterpretedAt();

            InvoiceFieldsDTO invoiceFields = interpretationResult.getInvoiceFields();
            if (invoiceFields != null) {
                extractedFields = Map.of(
                    "date", invoiceFields.getDate(),
                    "amount", invoiceFields.getAmount(),
                    "currency", invoiceFields.getCurrency(),
                    "sender", invoiceFields.getSender(),
                    "description", invoiceFields.getDescription()
                );
            }

            List<StatementTransaction> statementTransactions = interpretationResult.getStatementTransactions();
            if (statementTransactions != null && !statementTransactions.isEmpty()) {
                extractedTransactions = statementTransactions.stream()
                    .map(t -> new ExtractionResultResponseDTO.Transaction(
                        t.getDate() != null ? t.getDate().toString() : null,
                        t.getAmount(),
                        t.getCurrency(),
                        t.getDescription(),
                        null
                    ))
                    .collect(Collectors.toList());
            }
        }

        String documentType = correctedDocumentType;
        
        if ("STATEMENT".equals(documentType)) {
            return new ExtractionResultResponseDTO(
                documentId,
                "STATEMENT",
                1,
                extractedAt,
                null, // fields not used for STATEMENT
                correctedFields,
                null, // confidence not used for STATEMENT
                null, // warnings not used for STATEMENT
                extractedTransactions
            );
        }

        return new ExtractionResultResponseDTO(
            documentId,
            "INVOICE",
            1,
            extractedAt,
            extractedFields,
            correctedFields,
            null,
            null,
            null // transactions not used for INVOICE
        );
    }

    public DocumentCorrectionResponseDTO saveCorrection(String documentId, DocumentCorrectionRequestDTO request) {
        // Validate document exists
        Document document = getDocument(documentId);
        if (document == null) {
            throw new EntityNotFoundException("Document not found: " + documentId);
        }

        // Get next correction version
        Integer maxVersion = correctionRepository.findMaxCorrectionVersionByDocumentId(documentId).orElse(0);
        Integer nextVersion = maxVersion + 1;

        // Create correction
        OffsetDateTime correctionPlacedAt = OffsetDateTime.now();
        Instant now = correctionPlacedAt.toInstant();
        Correction correction = new Correction(
            null, // id will be generated
            documentId,
            request.getDocumentType(),
            request.getFields(),
            request.getNote(),
            nextVersion,
            now,
            correctionPlacedAt,
            "user:123", // Mock user ID
            1 // Mock normalized transactions created
        );

        // Save to database
        CorrectionEntity entity = CorrectionEntity.fromDomain(correction);
        correctionRepository.save(entity);

        return new DocumentCorrectionResponseDTO(
            documentId,
            nextVersion,
            now,
            correctionPlacedAt,
            "user:123",
            1
        );
    }
}