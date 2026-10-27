package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.pago.Respuesta2FA;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Servicio encargado de gestionar el doble factor de autenticación (2FA) por correo electrónico:
 * generación de códigos, envío por email y validación de seguridad.
 */
public interface Email2FAService {

    /**
     * Genera un código de 6 dígitos, lo almacena temporalmente y lo envía al correo del cliente.
     */
    Respuesta2FA generarYEnviarCodigo2FA(Integer idCliente, String numeroCuenta, String correoCliente, String nombreCliente, BigDecimal monto, String referencia);

    /**
     * Valida el código 2FA ingresado por el usuario.
     */
    boolean validarCodigo2FA(Integer idCliente, String numeroCuenta, String codigoIngresado);

    /**
     * Limpia un código 2FA tras su uso exitoso.
     */
    void invalidarCodigo2FA(Integer idCliente, String numeroCuenta);

    /**
     * Envía comprobante oficial de pago al correo del cliente.
     */
    boolean enviarComprobantePago(String correoCliente, String nombreCliente, String referencia, BigDecimal monto, String numeroCuenta, String nombreProducto, String transaccionId, Map<String, Object> ticket);
}
