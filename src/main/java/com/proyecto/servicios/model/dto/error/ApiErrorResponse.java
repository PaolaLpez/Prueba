package com.proyecto.servicios.model.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO para estructurar respuestas de error estandarizadas sin exponer datos sensibles
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private Integer status;
    private String error;
    private String mensaje;
    private LocalDateTime timestamp;
}
