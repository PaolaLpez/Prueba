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
public class TransferenciaResponse {

    private String folioTransferencia;
    private String numeroCuentaOrigen;
    private String numeroCuentaDestino;
    private String nombreDestinatario;
    private BigDecimal monto;
    private String concepto;
    private BigDecimal saldoNuevoOrigen;
    private BigDecimal saldoNuevoDestino;
    private LocalDateTime fechaOperacion;
    private String estatus;
    private String mensaje;
}
