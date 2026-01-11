package com.frnholding.pocketaccount;

import com.frnholding.pocketaccount.domain.Document;
import com.frnholding.pocketaccount.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("source") String source,
            @RequestParam("originalFilename") String originalFilename) {

        try {
            Document document = documentService.uploadDocument(file, source, originalFilename);
            DocumentUploadResponse response = new DocumentUploadResponse(
                    document.getId(),
                    document.getStatus(),
                    document.getCreated(),
                    document.getOriginalFilename()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}