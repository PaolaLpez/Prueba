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

    @NotBlank(message = "El número de cuenta es obligatorio")
    @JsonAlias({"numCuenta", "num_cuenta", "numero_cuenta", "account"})
    @Schema(description = "Número de cuenta bancaria activa del cliente titular", example = "5730094736", requiredMode = Schema.RequiredMode.REQUIRED)
    private String numeroCuenta;

    @NotNull(message = "El ID del cliente es obligatorio")
    @JsonAlias({"idCliente", "id_cliente", "clienteId", "cliente_id", "userId"})
    @Schema(description = "ID del cliente titular registrado en el sistema", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer idCliente;

    @NotBlank(message = "El ID del producto es obligatorio")
    @JsonAlias({"idProducto", "id_producto", "productoId", "producto_id", "productCode"})
    @Schema(description = "Identificador del producto en el catálogo oficial (debe existir y estar disponible)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idProducto;

    @JsonAlias({"idServicio", "id_servicio", "servicioId", "serviceId"})
    @Schema(description = "Identificador del servicio asociado al producto (opcional, si se envía se valida contra el catálogo)", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer idServicio;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @JsonAlias({"amount", "valor"})
    @Schema(description = "Monto monetario de la transacción. Debe coincidir estrictamente con el valor autorizado en el catálogo si el producto tiene precio fijo", example = "250.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;

    @NotBlank(message = "El teléfono es obligatorio")
    @JsonAlias({"cellphone", "phone", "number", "numeroTelefono"})
    @Schema(description = "Número telefónico asociado a la operación", example = "5512345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String telefono;

    @NotBlank(message = "La referencia de comprobante de pago es obligatoria")
    @JsonAlias({"reference", "referenciaPago", "comprobante", "trace"})
    @Schema(description = "Referencia única de la transacción (utilizada para idempotencia y prevención de duplicados)", example = "REF-PAOLA-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String referencia;

    @JsonAlias({"dobleAutenticacion", "dobleAutentificacion", "codigo2FA", "codigo2fa", "codigoDobleAutenticacion", "otp", "token2fa"})
    @Schema(description = "Código OTP de 6 dígitos enviado por Brevo al correo del cliente. Dejar null/vacío para solicitar el código; enviar el código recibido para confirmar la compra.", example = "623571", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String dobleAutenticacion;

    @JsonAlias({"correo", "email", "correoCliente", "correoDestino"})
    @Schema(description = "Correo electrónico destino para recibir el código 2FA y comprobante (opcional, si se omite se usa el correo registrado del cliente)", example = "isapaola188@gmail.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String correo;

    @JsonAlias({"unit", "cantidad"})
    @Builder.Default
    @Schema(description = "Unidad o cantidad de productos a adquirir", example = "1", defaultValue = "1")
    private String unidad = "1";
}
