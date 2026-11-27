package com.proyecto.servicios.model.dto.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para solicitar explícitamente el código de doble factor de autenticación (2FA) por correo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Solicitud2FARequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @JsonAlias({"idCliente", "id_cliente", "clienteId"})
    private Integer idCliente;

    @NotBlank(message = "El número de cuenta es obligatorio")
    @JsonAlias({"numCuenta", "num_cuenta", "numero_cuenta"})
    private String numeroCuenta;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @JsonAlias({"amount", "valor"})
    private BigDecimal monto;
}
