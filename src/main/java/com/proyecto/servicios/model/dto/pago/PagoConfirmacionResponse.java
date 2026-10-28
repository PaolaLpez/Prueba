package com.proyecto.servicios.model.dto.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de respuesta que confirma el pago exitoso según la especificación solicitada
 * y el estándar de respuestas transaccionales ("00" - Transacción Exitosa, "01" - 2FA Requerido, "02" - Pendiente de Conciliación).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta del procesamiento transaccional de pagos")
public class PagoConfirmacionResponse {

    @Schema(description = "Indica si la solicitud fue procesada exitosamente en el sistema", example = "true")
    private Boolean state;

    @Schema(description = "Código de respuesta operacional: '00' (Aprobada), '01' (2FA Requerido enviado a correo), '02' (Pendiente conciliación por timeout)", example = "00")
    private String code;

    @Schema(description = "Código numérico de estado", example = "0")
    private Integer codigo;

    @Schema(description = "Descripción del resultado de la operación", example = "Transacción exitosa. Pago confirmado y procesado con Gestopago")
    private String message;

    @Schema(description = "Mensaje alternativo para compatibilidad de clientes", example = "Transacción exitosa. Pago confirmado y procesado con Gestopago")
    private String mensaje;

    @Schema(description = "Detalle financiero, de auditoría y ticket digital de la transacción")
    private DetallePago data;

    public boolean isState() {
        return Boolean.TRUE.equals(state);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Detalle completo del pago confirmado")
    public static class DetallePago {
        @Schema(description = "Identificador único de la transacción en base de datos", example = "15")
        private String transaccionId;

        @Schema(description = "Código de seguimiento único de auditoría", example = "TX1730048123456")
        private String trace;

        @Schema(description = "Número de cuenta bancaria debitada", example = "5730094736")
        private String numeroCuenta;

        @Schema(description = "ID del cliente titular", example = "3")
        private Integer idCliente;

        @Schema(description = "Nombre completo del cliente", example = "Paola Lopez")
        private String nombreCliente;

        @Schema(description = "Correo electrónico enmascarado del cliente donde se entregó el código 2FA y comprobante Brevo", example = "lo****@gmail.com")
        private String correoCliente;

        @Schema(description = "ID del producto adquirido", example = "1")
        private String idProducto;

        @Schema(description = "Nombre comercial del producto en catálogo", example = "Recarga Telefónica")
        private String nombreProducto;

        @Schema(description = "Monto debitado", example = "250.00")
        private BigDecimal monto;

        @Schema(description = "Saldo disponible previo a la transacción", example = "2500.00")
        private BigDecimal saldoAnterior;

        @Schema(description = "Saldo disponible actualizado tras el débito", example = "2250.00")
        private BigDecimal saldoDisponibleActual;

        @Schema(description = "Teléfono de referencia", example = "5512345678")
        private String telefono;

        @Schema(description = "Referencia única enviada", example = "REF-PAOLA-001")
        private String referencia;

        @Schema(description = "Estampa temporal local de la transacción", example = "2026-10-27 10:45:12")
        private String horaLocal;

        @Schema(description = "Código UPC de 10 dígitos generado", example = "8472910482")
        private String upc;

        @Schema(description = "Unidad del producto", example = "1")
        private String unidad;

        @Schema(description = "Payload cifrado con algoritmo AES para integridad y no repudio", example = "QzZDNTRBMkY...")
        private String peticionCifrada;

        @Schema(description = "Token Bearer utilizado con el proveedor externo enmascarado para seguridad", example = "eyJhbG...9a8b")
        private String token;

        @Schema(description = "Estado transaccional: EXITOSA, PENDIENTE_CONCILIACION, PENDIENTE_CODIGO_2FA", example = "EXITOSA")
        private String estadoTransaccion;

        @Schema(description = "Fecha y hora de creación del registro")
        private LocalDateTime fechaTransaccion;

        @Schema(description = "Estructura del comprobante digital / tirilla de pago enviada al correo")
        private Map<String, Object> ticket;
    }
}
