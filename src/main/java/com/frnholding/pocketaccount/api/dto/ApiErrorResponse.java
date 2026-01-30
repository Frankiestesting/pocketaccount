package com.frnholding.pocketaccount.api.dto;

import java.util.Map;

public class ApiErrorResponse {
    private String code;
    private String message;
    private Map<String, String> details;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiErrorResponse(String code, String message, Map<String, String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
