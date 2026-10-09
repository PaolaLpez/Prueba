package com.proyecto.servicios.model.dto.seguridad;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String token;
    private String tokenType;
    private Integer expiresInSeconds; // Configurado a 300 segundos (5 minutos de inactividad)
    private String mensajeInactividad;
    private Integer clienteId;
    private String nombreCliente;
    private LocalDateTime fechaAutenticacion;
}
