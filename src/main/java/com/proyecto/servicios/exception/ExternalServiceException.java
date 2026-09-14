package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Excepción personalizada para gestionar fallos en servicios de integración externos
@Getter
public class ExternalServiceException extends RuntimeException {

    private final HttpStatus status;

    public ExternalServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ExternalServiceException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
}
