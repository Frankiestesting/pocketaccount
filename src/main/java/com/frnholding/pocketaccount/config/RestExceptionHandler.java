package com.frnholding.pocketaccount.config;

import com.frnholding.pocketaccount.api.dto.ApiErrorResponse;
import com.frnholding.pocketaccount.exception.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ApiErrorResponse error = new ApiErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            fieldErrors
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
            "NOT_FOUND",
            ex.getMessage()
        );
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
            "CONFLICT",
            "Data integrity violation: " + extractConstraintMessage(ex)
        );
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
            "BAD_REQUEST",
            ex.getMessage()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
            "BAD_REQUEST",
            ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErrorResponse> handleIoException(IOException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
            "IO_ERROR",
            "I/O failure processing the request"
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String extractConstraintMessage(DataIntegrityViolationException ex) {
        if (ex.getCause() != null) {
            String message = ex.getCause().getMessage();
            if (message != null && message.contains("unique")) {
                return "Duplicate entry";
            }
            return message;
        }
        return "Database constraint violation";
    }
}
