package com.mediaalterations.mainservice.exceptions;

import com.mediaalterations.mainservice.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> handleExternalServiceException(ExternalServiceException ex){
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.builder()
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .errorMessage(ex.getMessage())
                        .errorClass(ex.getClass().getName())
                        .build());
    }
    @ExceptionHandler(ProcessCreationException.class)
    public ResponseEntity<ApiError> handleProcessCreationException(ProcessCreationException ex){
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .errorMessage(ex.getMessage())
                        .errorClass(ex.getClass().getName())
                        .build());
    }
    @ExceptionHandler(ProcessNotFoundException.class)
    public ResponseEntity<ApiError> handleProcessNotFoundException(ProcessNotFoundException ex){
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .errorMessage(ex.getMessage())
                        .errorClass(ex.getClass().getName())
                        .build());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex){
        ex.printStackTrace();
        return ResponseEntity
                .status(500)
                .body(ApiError.builder()
                        .status(500)
                        .errorMessage(ex.getMessage())
                        .errorClass(ex.getClass().getName())
                        .build());
    }
}
