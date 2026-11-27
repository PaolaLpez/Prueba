package com.proyecto.servicios.model.dto.gestopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para encapsular la respuesta analizada y tipada de la operación financiera sendTx.do de GestoPago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoTxResponse {

    private boolean exitoso;
    private boolean timeout;
    private Integer codigo;
    private String status;
    private String mensaje;
    private String folio;
    private String autorizacion;
    private String fecha;
    private String hora;
    private String rawXml;
    private LocalDateTime fechaOperacion;
}
