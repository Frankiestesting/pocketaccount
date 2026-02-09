package com.frnholding.pocketaccount.interpretation.infra;

public class OpenAiAuthenticationException extends RuntimeException {
    public OpenAiAuthenticationException(String message) {
        super(message);
    }

    public OpenAiAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
