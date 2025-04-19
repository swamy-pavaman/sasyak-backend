package com.kapilagro.sasyak.aop;

import com.kapilagro.sasyak.exceptions.TenantAlreadyExistsException;
import com.kapilagro.sasyak.model.CreateTenantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ResponseEntity<CreateTenantResponse> handleTenantExists(TenantAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CreateTenantResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CreateTenantResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CreateTenantResponse.builder()
                        .message("An unexpected error occurred.")
                        .build());
    }
}

