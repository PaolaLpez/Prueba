package com.proyecto.servicios.model.dto.puntored;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para autenticación en la API de Puntored según documentación:
 * POST https://services.preprodcxr.co/puntored/api/v1/auth
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PuntoredAuthRequest {

    private String username;
    private String password;
    private Integer commerce;
}
