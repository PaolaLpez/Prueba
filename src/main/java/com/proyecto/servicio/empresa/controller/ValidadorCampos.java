package com.proyecto.servicio.empresa.controller;

import com.proyecto.servicio.empresa.model.response.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ValidadorCampos {

    /**
     * En caso de que alguna validacion no se cumpla para algun DTO se tira MethodArgumentNotValidException
     * este metodo la cacha y regresa los mensajes de error para el cliente, con los message de cada campo.
     * @param errors
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> dtoValidation(MethodArgumentNotValidException ex) {
        GenericResponse genericResponse = new GenericResponse();
        Map<String, String> listError = new HashMap<>();
        if (ex.getBindingResult() != null) {
            for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
                listError.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        }
        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no válidos: " + listError);
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }
}
