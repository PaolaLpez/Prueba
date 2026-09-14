package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.dto.error.ApiErrorResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Optional;

// Manejador global de excepciones que procesa errores de peticiones e integración de forma declarativa (sin sentencias if)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Captura errores de validación de peticiones (@Valid) directamente sin usar bloques if
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String detalleError = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Parámetros de petición inválidos");
        
        log.warn("Petición inválida capturada: {}", detalleError);
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Petición Inválida")
                .mensaje(detalleError)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Captura excepciones personalizadas emitidas por la capa de integración de servicios
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalServiceException(ExternalServiceException ex) {
        log.error("Error en servicio externo: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // Maneja declarativamente errores de autenticación (401/403) devueltos por el cliente Feign
    @ExceptionHandler({FeignException.Unauthorized.class, FeignException.Forbidden.class})
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(FeignException ex) {
        log.error("Error de autenticación en servicio externo. Código HTTP: {}", ex.status());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Error de Autenticación")
                .mensaje("Fallo de autenticación con el servicio externo")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // Maneja declarativamente errores de timeout y comunicación con el servicio externo
    @ExceptionHandler({FeignException.GatewayTimeout.class, RetryableException.class})
    public ResponseEntity<ApiErrorResponse> handleTimeoutException(Exception ex) {
        log.error("Timeout de comunicación con el servicio externo: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.GATEWAY_TIMEOUT.value())
                .error("Tiempo de Espera Agotado")
                .mensaje("El servicio externo no respondió dentro del tiempo esperado")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    // Captura respuestas HTTP no exitosas genéricas retornadas por OpenFeign
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiErrorResponse> handleFeignException(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        HttpStatus finalStatus = Optional.ofNullable(status).orElse(HttpStatus.BAD_GATEWAY);

        log.error("Respuesta no exitosa del servicio externo. Código: {}", ex.status());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(finalStatus.value())
                .error(finalStatus.getReasonPhrase())
                .mensaje("Error al procesar la respuesta del servicio externo")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(finalStatus).body(response);
    }

    // Captura de forma global cualquier otra excepción no controlada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Error inesperado en el sistema: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Error Interno del Servidor")
                .mensaje("Ha ocurrido un error inesperado al procesar la solicitud")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
