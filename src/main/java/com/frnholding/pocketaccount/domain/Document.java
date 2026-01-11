package com.frnholding.pocketaccount.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String id;
    private String status;
    private Instant created;
    private String originalFilename;
    private String filePath;
}