package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.gestopago.GestoPagoTxResponse;

import java.math.BigDecimal;

/**
 * Servicio de integración transaccional con la API sendTx.do de GestoPago.
 * Implementa control de errores, manejo seguro de Bearer Token, protección contra timeouts
 * y parseo seguro de respuestas XML.
 */
public interface GestoPagoTxService {

    /**
     * Envía una transacción a GestoPago POST /sistema/service/sendTx.do utilizando Bearer Token.
     */
    GestoPagoTxResponse enviarTransaccion(Integer idServicio, Integer idProducto, BigDecimal monto, String referencia, String upc, String unidad);
}
