package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.pago.PagoConfirmacionResponse;
import com.proyecto.servicios.model.dto.pago.PagoRequest;

/**
 * Contrato de negocio para procesar pagos seguros con validación de cliente, saldo,
 * doble autenticación 2FA por correo, cifrado de datos y confirmación del pago.
 */
public interface PagoService {

    /**
     * Procesa la compra / pago validando cuenta, cliente activo, saldo, referencia,
     * doble factor de autenticación 2FA, empaqueta los datos del usuario (id producto,
     * id cliente, teléfono, referencia, hora local, monto, upc de 10 dígitos, unidad),
     * los cifra en una petición AES, obtiene el token de autenticación (Puntored/GestoPago),
     * envía la estructura cifrada más el token y confirma el pago exitoso en una sola petición.
     */
    PagoConfirmacionResponse procesarPago(PagoRequest request);
}
