package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends RuntimeException {

    private final HttpStatus status;

    public RecursoNoEncontradoException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
