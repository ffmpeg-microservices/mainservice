package com.mediaalterations.mainservice.exceptions;



public class ProcessNotFoundException extends RuntimeException {
    public ProcessNotFoundException(String message) {
        super(message);
    }
}