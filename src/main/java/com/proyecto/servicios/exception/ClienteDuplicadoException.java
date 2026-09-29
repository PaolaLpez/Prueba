package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteDuplicadoException extends RuntimeException {

    private final HttpStatus status;

    public ClienteDuplicadoException(String message) {
        super(message);
        this.status = HttpStatus.CONFLICT;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
