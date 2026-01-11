package com.example.demo;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class DocumentController {

    private static final List<String> ALLOWED_SOURCES = Arrays.asList("mobile", "web");
    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("source") String source,
            @RequestParam("originalFilename") String originalFilename) {

        try {
            // Validate source
            if (!ALLOWED_SOURCES.contains(source)) {
                return ResponseEntity.badRequest().build();
            }

            // Validate file is PDF
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            String contentType = file.getContentType();
            if (contentType != null && !contentType.equals("application/pdf")) {
                // Allow if extension is pdf
                String filename = file.getOriginalFilename();
                if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                    return ResponseEntity.badRequest().build();
                }
            }

            // Generate documentId
            String documentId = UUID.randomUUID().toString();

            // Save file
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(documentId + ".pdf");
            Files.copy(file.getInputStream(), filePath);

            // Create response
            DocumentUploadResponse response = new DocumentUploadResponse(
                    documentId,
                    "uploaded",
                    Instant.now(),
                    originalFilename
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}