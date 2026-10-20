package com.proyecto.servicios.model.dto.cuenta;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CuentaResponse {

    private Integer id;
    private String numeroCuenta;
    private String estatus;
    private BigDecimal saldoDisponible;
    private BigDecimal saldoRetenido;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaActualizacionSaldo;
}
