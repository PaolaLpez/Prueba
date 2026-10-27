package com.proyecto.servicios.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Servicio para gestión del envío de correos transaccionales reales usando la API de Brevo.
 */
public interface BrevoEmailService {

    /**
     * Envía un correo con el código de seguridad 2FA para autorizar una compra o pago.
     */
    boolean enviarCodigo2FA(String destinatario, String nombre, String codigo, BigDecimal monto, String referencia);

    /**
     * Envía el comprobante digital de compra y ticket tras completarse la transacción exitosamente.
     */
    boolean enviarComprobantePago(String destinatario, String nombre, String referencia, BigDecimal monto, String numeroCuenta, String nombreProducto, String transaccionId, Map<String, Object> ticket);

    /**
     * Envío genérico de correo electrónico con soporte HTML y texto plano.
     */
    boolean enviarCorreo(String destinatario, String nombreDestinatario, String asunto, String contenidoHtml, String contenidoTexto);
}
