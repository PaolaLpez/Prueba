package com.proyecto.servicios.model.dto.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la emisión del código de doble factor de autenticación (2FA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Respuesta2FA {

    private Boolean state;
    private String code;
    private Integer codigo;
    private String message;
    private String mensaje;
    private String correoDestino;
    private Integer tiempoValidezSegundos;
    private LocalDateTime fechaExpiracion;
}
