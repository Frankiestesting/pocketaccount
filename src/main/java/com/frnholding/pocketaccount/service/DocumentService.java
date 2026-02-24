package com.frnholding.pocketaccount.service;

import com.frnholding.pocketaccount.api.dto.DocumentCorrectionRequestDTO;
import com.frnholding.pocketaccount.api.dto.DocumentCorrectionResponseDTO;
import com.frnholding.pocketaccount.api.dto.ExtractionResultResponseDTO;
import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.domain.DocumentEntity;
import com.frnholding.pocketaccount.domain.Job;
import com.frnholding.pocketaccount.domain.JobEntity;
import com.frnholding.pocketaccount.exception.EntityNotFoundException;
import com.frnholding.pocketaccount.exception.ConflictException;
import com.frnholding.pocketaccount.repository.DocumentRepository;
import com.frnholding.pocketaccount.repository.JobRepository;
import com.frnholding.pocketaccount.interpretation.domain.InterpretationResult;
import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;
import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.repository.InterpretationResultRepository;
import com.frnholding.pocketaccount.interpretation.repository.StatementTransactionRepository;
import com.frnholding.pocketaccount.interpretation.service.InterpretationService;
import com.frnholding.pocketaccount.accounting.domain.ReceiptMatchStatus;
import com.frnholding.pocketaccount.accounting.repository.ReceiptRepository;
import com.frnholding.pocketaccount.accounting.repository.ReceiptMatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private InterpretationResultRepository interpretationResultRepository;

    @Autowired
    private StatementTransactionRepository statementTransactionRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptMatchRepository receiptMatchRepository;

    @Autowired
    @Lazy
    private InterpretationService interpretationService;

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

    @Transactional
    public void deleteDocument(String documentId) {
        DocumentEntity entity = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        if (statementTransactionRepository.existsByInterpretationResult_DocumentIdAndApprovedTrue(documentId)) {
            throw new ConflictException("Cannot delete document: approved statement transactions exist");
        }

        if (isReceiptApproved(documentId)) {
            throw new ConflictException("Cannot delete document: receipt is matched and locked");
        }

        String filePath = entity.getFilePath();
        documentRepository.delete(entity);

        if (filePath != null && !filePath.isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to delete document file: " + filePath, e);
            }
        }
    }

    private boolean isReceiptApproved(String documentId) {
        try {
            UUID receiptDocumentId = UUID.fromString(documentId);
                return receiptRepository.findByDocumentId(receiptDocumentId)
                    .map(receipt -> receiptMatchRepository.existsByReceiptIdAndStatus(receipt.getId(), ReceiptMatchStatus.ACTIVE))
                    .orElse(false);
        } catch (IllegalArgumentException ex) {
            return false;
        }
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

    @Transactional
    public void deleteJob(String jobId) {
        JobEntity entity = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));
        jobRepository.delete(entity);
    }

    public ExtractionResultResponseDTO getExtractionResult(String documentId) {
        // Validate document exists
        Document document = getDocument(documentId);
        if (document == null) {
            throw new EntityNotFoundException("Document not found: " + documentId);
        }

        InterpretationResult interpretationResult = interpretationResultRepository
            .findByDocumentId(documentId)
            .orElse(null);

        Map<String, Object> extractedFields = null;
        List<ExtractionResultResponseDTO.Transaction> extractedTransactions = null;
        Instant extractedAt = null;
        List<String> warnings = new java.util.ArrayList<>();

        if (interpretationResult != null) {
            extractedAt = interpretationResult.getInterpretedAt();

            InvoiceFieldsDTO invoiceFields = interpretationResult.getInvoiceFields();
            if (invoiceFields != null) {
                Map<String, Object> fields = new java.util.LinkedHashMap<>();
                fields.put("date", invoiceFields.getDate());
                fields.put("amount", invoiceFields.getAmount());
                fields.put("currency", invoiceFields.getCurrency());
                fields.put("sender", invoiceFields.getSender());
                fields.put("description", invoiceFields.getDescription());
                extractedFields = fields;

                if (invoiceFields.getDate() == null) {
                    warnings.add("Missing field: date");
                }
                if (invoiceFields.getAmount() == null) {
                    warnings.add("Missing field: amount");
                }
                if (invoiceFields.getCurrency() == null) {
                    warnings.add("Missing field: currency");
                }
                if (invoiceFields.getSender() == null) {
                    warnings.add("Missing field: sender");
                }
                if (invoiceFields.getDescription() == null) {
                    warnings.add("Missing field: description");
                }
            }

            List<StatementTransaction> statementTransactions = interpretationResult.getStatementTransactions();
            if (statementTransactions != null && !statementTransactions.isEmpty()) {
                int index = 0;
                extractedTransactions = statementTransactions.stream()
                    .map(t -> new ExtractionResultResponseDTO.Transaction(
                        t.getDate() != null ? t.getDate().toString() : null,
                        t.getAmount(),
                        t.getCurrency(),
                        t.getDescription(),
                        null,
                        t.getAccountNo(),
                        t.isApproved()
                    ))
                    .collect(Collectors.toList());

                for (StatementTransaction t : statementTransactions) {
                    index++;
                    if (t.getDate() == null) {
                        warnings.add("Transaction " + index + " missing date");
                    }
                    if (t.getAmount() == null) {
                        warnings.add("Transaction " + index + " missing amount");
                    }
                    if (t.getCurrency() == null) {
                        warnings.add("Transaction " + index + " missing currency");
                    }
                    if (t.getDescription() == null) {
                        warnings.add("Transaction " + index + " missing description");
                    }
                }
            } else if ("STATEMENT".equals(document.getDocumentType())) {
                warnings.add("No transactions extracted");
            }
        } else {
            warnings.add("No interpretation result found");
        }

        String documentType = document.getDocumentType();
        if (interpretationResult != null && interpretationResult.getDocumentType() != null) {
            documentType = interpretationResult.getDocumentType();
        }
        
        if ("STATEMENT".equals(documentType)) {
            return new ExtractionResultResponseDTO(
                documentId,
                "STATEMENT",
                1,
                extractedAt,
                null, // fields not used for STATEMENT
                null, // corrected fields not used for STATEMENT
                null, // confidence not used for STATEMENT
                warnings.isEmpty() ? null : warnings,
                extractedTransactions
            );
        }

        return new ExtractionResultResponseDTO(
            documentId,
            documentType,
            1,
            extractedAt,
            extractedFields,
            null,
            null,
            warnings.isEmpty() ? null : warnings,
            null // transactions not used for INVOICE/RECEIPT
        );
    }

    public DocumentCorrectionResponseDTO saveCorrection(String documentId, DocumentCorrectionRequestDTO request) {
        Integer correctionVersion = interpretationService.saveCorrection(documentId, request.toInterpretationRequest());
        Instant now = Instant.now();
        return new DocumentCorrectionResponseDTO(
            documentId,
            correctionVersion,
            now,
            now.atOffset(java.time.ZoneOffset.UTC),
            "user:123",
            null
        );
    }
}