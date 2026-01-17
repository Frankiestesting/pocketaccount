package com.frnholding.pocketaccount.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCorrectionRequestDTO {
    private String documentType;
    private Map<String, Object> fields;
    private String note;
}