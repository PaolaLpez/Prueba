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

    // Captura errores de validación de peticiones (@Valid) directamente reportando todos los campos inválidos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String detalleError = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining("; "));
        
        if (detalleError == null || detalleError.isBlank()) {
            detalleError = "Parámetros de petición inválidos";
        }
        
        log.warn("Petición inválida capturada: {}", detalleError);
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Petición Inválida")
                .mensaje(detalleError)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Captura errores de formato JSON, tipos de datos incompatibles o fechas mal formateadas (por ejemplo con diagonales)
    @ExceptionHandler({
        org.springframework.http.converter.HttpMessageNotReadableException.class, 
        java.time.format.DateTimeParseException.class,
        com.fasterxml.jackson.databind.exc.InvalidFormatException.class
    })
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(Exception ex) {
        log.warn("Error al procesar formato JSON de la petición: {}", ex.getMessage());

        String mensaje = "El formato de los datos en la petición es inválido";
        String textoCompleto = (ex.getMessage() != null ? ex.getMessage() : "") + " "
                + (ex.getCause() != null && ex.getCause().getMessage() != null ? ex.getCause().getMessage() : "");
        
        if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException readableEx && readableEx.getMostSpecificCause() != null && readableEx.getMostSpecificCause().getMessage() != null) {
            textoCompleto += " " + readableEx.getMostSpecificCause().getMessage();
        }

        String textoLower = textoCompleto.toLowerCase();
        if (textoLower.contains("localdate") || textoLower.contains("fechanacimiento") || textoLower.contains("datetimeparseexception") || textoLower.contains("date") || textoLower.contains("fecha")) {
            mensaje = "El formato de la fecha es inválido. Debe ser estrictamente YYYY-MM-DD (ejemplo: 1995-05-15) sin diagonales ni otro formato";
        } else if (textoLower.contains("bigdecimal") || textoLower.contains("ingresomensual") || textoLower.contains("saldoinicial") || textoLower.contains("numberformatexception") || textoLower.contains("numeric")) {
            mensaje = "El campo numérico contiene un formato inválido. Solo se permiten números sin letras ni caracteres extraños";
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Formato Inválido")
                .mensaje(mensaje)
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

    // Captura excepciones de duplicación de datos de cliente (CURP, RFC, correo)
    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<ApiErrorResponse> handleClienteDuplicadoException(ClienteDuplicadoException ex) {
        log.warn("Registro de cliente duplicado rechazado: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(ex.getStatus().value())
                .error("Cliente Duplicado")
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // Captura violaciones de unicidad a nivel de restricciones de base de datos
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Violación de restricción única en BD: {}", ex.getMessage());

        String mensaje = "Ya existe un registro con estos datos únicos en el sistema";
        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
            String causa = ex.getMostSpecificCause().getMessage().toLowerCase();
            if (causa.contains("curp")) {
                mensaje = "Ya existe un cliente registrado con esta CURP";
            } else if (causa.contains("rfc")) {
                mensaje = "Ya existe un cliente registrado con este RFC";
            } else if (causa.contains("correo") || causa.contains("username")) {
                mensaje = "Ya existe un cliente o usuario registrado con este correo electrónico";
            } else if (causa.contains("numero_cuenta")) {
                mensaje = "El número de cuenta generado ya existe";
            }
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Cliente Duplicado")
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // Captura excepciones de recurso no encontrado (Cliente, Cuenta, etc.)
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleRecursoNoEncontradoException(RecursoNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(ex.getStatus().value())
                .error("Recurso No Encontrado")
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // Captura excepciones de violación de reglas de negocio de clientes y cuentas
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiErrorResponse> handleReglaNegocioException(ReglaNegocioException ex) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(ex.getStatus().value())
                .error("Violación de Regla de Negocio")
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
        HttpStatus finalStatus = (status != null && status.isError()) ? status : HttpStatus.BAD_GATEWAY;

        log.error("Respuesta no exitosa del servicio externo. Código HTTP original: {}", ex.status());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(finalStatus.value())
                .error(finalStatus.getReasonPhrase())
                .mensaje("Error al procesar la respuesta del servicio externo")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(finalStatus).body(response);
    }

    // Captura saturación temporal del pool de conexiones a la base de datos (HikariCP / PostgreSQL)
    @ExceptionHandler({
        org.springframework.dao.DataAccessResourceFailureException.class,
        org.springframework.transaction.CannotCreateTransactionException.class,
        org.springframework.jdbc.CannotGetJdbcConnectionException.class
    })
    public ResponseEntity<ApiErrorResponse> handleDatabaseSaturationException(Exception ex) {
        log.warn("Saturación temporal de base de datos detectada bajo alta carga: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Servicio Temporalmente No Disponible")
                .mensaje("El servidor está experimentando alta demanda en la base de datos. Por favor reintente su solicitud en unos momentos.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
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
