package com.proyecto.servicios.model.dto.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para mapear la respuesta del endpoint GET /sistema/service/sendEcho.do
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoEchoResponse {

    private Integer codigo;
    private String mensaje;
    private String status;
    private String echo;
}
