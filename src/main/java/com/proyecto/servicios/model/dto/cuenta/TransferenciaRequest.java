package com.proyecto.servicios.model.dto.cuenta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferenciaRequest {

    @NotBlank(message = "El número de cuenta de origen es obligatorio")
    private String numeroCuentaOrigen;

    @NotBlank(message = "El número de cuenta de destino es obligatorio")
    private String numeroCuentaDestino;

    @NotNull(message = "El monto a transferir es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto mínimo a transferir debe ser mayor a 0")
    private BigDecimal monto;

    private String concepto;
}
