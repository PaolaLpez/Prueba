package com.proyecto.servicios.model.dto.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa la envoltura de envío que contiene la petición cifrada más el token de autenticación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeticionCifradaPago {

    private String cifrado;
    private String token;
    private String tokenType;
    private String trace;
}
