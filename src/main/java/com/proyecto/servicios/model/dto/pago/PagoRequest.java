package com.proyecto.servicios.model.dto.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la solicitud de compra / pago con validaciones completas,
 * documentación OpenAPI y aliases flexibles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"correo", "nombreProducto", "monto", "telefono", "dobleAutenticacion"})
@Schema(description = "Payload de entrada para procesar o confirmar una transacción de pago")
public class PagoRequest {

    @JsonAlias({"numCuenta", "num_cuenta", "numero_cuenta", "account"})
    @Schema(hidden = true)
    private String numeroCuenta;

    @JsonAlias({"idCliente", "id_cliente", "clienteId", "cliente_id", "userId"})
    @Schema(hidden = true)
    private Integer idCliente;

    @JsonAlias({"idProducto", "id_producto", "productoId", "producto_id", "productCode"})
    @Schema(hidden = true)
    private String idProducto;

    @JsonAlias({"nombreProducto", "producto", "nombre_producto", "productName", "nombre"})
    @Schema(description = "Nombre o descripción del producto en el catálogo oficial", example = "Recarga Telefónica y Pagos de Servicios", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nombreProducto;

    @JsonAlias({"idServicio", "id_servicio", "servicioId", "serviceId"})
    @Schema(hidden = true)
    private Integer idServicio;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @JsonAlias({"amount", "valor"})
    @Schema(description = "Monto monetario de la transacción", example = "250.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private BigDecimal monto;

    @JsonAlias({"cellphone", "phone", "number", "numeroTelefono"})
    @Schema(description = "Número telefónico asociado a la operación", example = "5512345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String telefono;

    @JsonAlias({"reference", "referenciaPago", "comprobante", "trace"})
    @Schema(hidden = true)
    private String referencia;

    @JsonAlias({"dobleAutenticacion", "dobleAutentificacion", "codigo2FA", "codigo2fa", "codigoDobleAutenticacion", "otp", "token2fa"})
    @Schema(description = "Código OTP de 6 dígitos enviado por correo electrónico. Dejar null/vacío para solicitar el código; enviar el código recibido para confirmar la compra.", example = "853927", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String dobleAutenticacion;

    @JsonAlias({"correo", "email", "correoCliente", "correoDestino"})
    @Schema(description = "Correo electrónico registrado de la cuenta. Permite identificar automáticamente al cliente y su cuenta bancaria activa.", example = "lopezpao869@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correo;

    @JsonAlias({"unit", "cantidad"})
    @Builder.Default
    @Schema(hidden = true)
    private String unidad = "1";
}
