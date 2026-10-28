package com.proyecto.servicios.model.dto.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Payload de entrada para procesar o confirmar una transacción de pago")
public class PagoRequest {

    @JsonAlias({"numCuenta", "num_cuenta", "numero_cuenta", "account"})
    @Schema(description = "Número de cuenta bancaria activa del cliente titular (opcional si se provee correo)", example = "5730094736", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String numeroCuenta;

    @JsonAlias({"idCliente", "id_cliente", "clienteId", "cliente_id", "userId"})
    @Schema(description = "ID del cliente titular registrado en el sistema (opcional si se provee correo)", example = "3", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer idCliente;

    @JsonAlias({"idProducto", "id_producto", "productoId", "producto_id", "productCode"})
    @Schema(description = "Identificador del producto en el catálogo oficial (opcional si se envía nombreProducto)", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String idProducto;

    @JsonAlias({"nombreProducto", "producto", "nombre_producto", "productName", "nombre"})
    @Schema(description = "Nombre o descripción del producto en el catálogo oficial (si se envía, se obtiene automáticamente su ID, servicio y precio)", example = "Recarga Telefónica y Pagos de Servicios", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nombreProducto;

    @JsonAlias({"idServicio", "id_servicio", "servicioId", "serviceId"})
    @Schema(description = "Identificador del servicio asociado al producto (opcional, si se envía se valida contra el catálogo o se obtiene automáticamente)", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer idServicio;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @JsonAlias({"amount", "valor"})
    @Schema(description = "Monto monetario de la transacción (opcional si el producto tiene precio fijo en el catálogo)", example = "250.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private BigDecimal monto;

    @JsonAlias({"cellphone", "phone", "number", "numeroTelefono"})
    @Schema(description = "Número telefónico asociado a la operación (opcional, se toma automáticamente del cliente registrado)", example = "5512345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String telefono;

    @JsonAlias({"reference", "referenciaPago", "comprobante", "trace"})
    @Schema(description = "Referencia única de la transacción (opcional, si no se envía se genera automáticamente de forma única)", example = "REF-PAOLA-005", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String referencia;

    @JsonAlias({"dobleAutenticacion", "dobleAutentificacion", "codigo2FA", "codigo2fa", "codigoDobleAutenticacion", "otp", "token2fa"})
    @Schema(description = "Código OTP de 6 dígitos enviado por correo electrónico. Dejar null/vacío para solicitar el código; enviar el código recibido para confirmar la compra.", example = "623571", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String dobleAutenticacion;

    @JsonAlias({"correo", "email", "correoCliente", "correoDestino"})
    @Schema(description = "Correo electrónico registrado de la cuenta. Permite identificar automáticamente al cliente y su cuenta bancaria activa.", example = "lopezpao869@gmail.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String correo;

    @JsonAlias({"unit", "cantidad"})
    @Builder.Default
    @Schema(description = "Unidad o cantidad de productos a adquirir", example = "1", defaultValue = "1")
    private String unidad = "1";
}
