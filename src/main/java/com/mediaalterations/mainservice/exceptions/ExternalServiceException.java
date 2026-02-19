package com.mediaalterations.mainservice.exceptions;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}