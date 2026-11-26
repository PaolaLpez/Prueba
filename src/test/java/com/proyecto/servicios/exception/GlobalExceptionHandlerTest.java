package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.dto.error.ApiErrorResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Debe manejar RecursoNoEncontradoException con HTTP 404")
    void testHandleRecursoNoEncontradoException() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("No encontrado");
        ResponseEntity<ApiErrorResponse> response = handler.handleRecursoNoEncontradoException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No encontrado", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar ReglaNegocioException con HTTP 422")
    void testHandleReglaNegocioException() {
        ReglaNegocioException ex = new ReglaNegocioException("Regla rota");
        ResponseEntity<ApiErrorResponse> response = handler.handleReglaNegocioException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Regla rota", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar ClienteDuplicadoException con HTTP 409")
    void testHandleClienteDuplicadoException() {
        ClienteDuplicadoException ex = new ClienteDuplicadoException("Cliente ya existe");
        ResponseEntity<ApiErrorResponse> response = handler.handleClienteDuplicadoException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar ExternalServiceException")
    void testHandleExternalServiceException() {
        ExternalServiceException ex = new ExternalServiceException("Error externo", HttpStatus.BAD_GATEWAY);
        ResponseEntity<ApiErrorResponse> response = handler.handleExternalServiceException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar IllegalArgumentException")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");
        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgumentException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar HttpMessageNotReadableException con distintas causas")
    void testHandleHttpMessageNotReadableException() {
        Exception exFecha = new Exception("Error en fecha DateTimeParseException localdate");
        ResponseEntity<ApiErrorResponse> responseFecha = handler.handleHttpMessageNotReadableException(exFecha);
        assertEquals(HttpStatus.BAD_REQUEST, responseFecha.getStatusCode());

        Exception exNumero = new Exception("Error en campo bigdecimal numberformatexception");
        ResponseEntity<ApiErrorResponse> responseNumero = handler.handleHttpMessageNotReadableException(exNumero);
        assertEquals(HttpStatus.BAD_REQUEST, responseNumero.getStatusCode());

        Exception exOtro = new Exception("Otro error general de formato");
        ResponseEntity<ApiErrorResponse> responseOtro = handler.handleHttpMessageNotReadableException(exOtro);
        assertEquals(HttpStatus.BAD_REQUEST, responseOtro.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar DataIntegrityViolationException con distintas causas")
    void testHandleDataIntegrityViolationException() {
        DataIntegrityViolationException exCurp = new DataIntegrityViolationException("curp duplicate", new Throwable("curp"));
        assertEquals(HttpStatus.CONFLICT, handler.handleDataIntegrityViolationException(exCurp).getStatusCode());

        DataIntegrityViolationException exRfc = new DataIntegrityViolationException("rfc duplicate", new Throwable("rfc"));
        assertEquals(HttpStatus.CONFLICT, handler.handleDataIntegrityViolationException(exRfc).getStatusCode());

        DataIntegrityViolationException exCorreo = new DataIntegrityViolationException("correo duplicate", new Throwable("correo"));
        assertEquals(HttpStatus.CONFLICT, handler.handleDataIntegrityViolationException(exCorreo).getStatusCode());

        DataIntegrityViolationException exCuenta = new DataIntegrityViolationException("cuenta duplicate", new Throwable("numero_cuenta"));
        assertEquals(HttpStatus.CONFLICT, handler.handleDataIntegrityViolationException(exCuenta).getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar FeignException de autenticación y genérica")
    void testHandleFeignException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api", new HashMap<>(), null, new RequestTemplate());
        FeignException.Unauthorized exAuth = new FeignException.Unauthorized("unauth", request, null, Collections.emptyMap());
        ResponseEntity<ApiErrorResponse> respAuth = handler.handleAuthenticationException(exAuth);
        assertEquals(HttpStatus.UNAUTHORIZED, respAuth.getStatusCode());

        FeignException exBadRequest = new FeignException.BadRequest("error", request, null, Collections.emptyMap());
        ResponseEntity<ApiErrorResponse> response = handler.handleFeignException(exBadRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar TimeoutException")
    void testHandleTimeoutException() {
        Exception ex = new Exception("timeout");
        ResponseEntity<ApiErrorResponse> response = handler.handleTimeoutException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar DatabaseSaturationException")
    void testHandleDatabaseSaturationException() {
        Exception ex = new Exception("pool exhausted");
        ResponseEntity<ApiErrorResponse> response = handler.handleDatabaseSaturationException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar Exception general con HTTP 500")
    void testHandleGenericException() {
        Exception ex = new Exception("Error inesperado");
        ResponseEntity<ApiErrorResponse> response = handler.handleGenericException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
