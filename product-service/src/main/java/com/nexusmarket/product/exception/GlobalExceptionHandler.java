package com.nexusmarket.product.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleProductNotFound(RuntimeException ex) {
        // We wrap the error message and the 404 status together
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}