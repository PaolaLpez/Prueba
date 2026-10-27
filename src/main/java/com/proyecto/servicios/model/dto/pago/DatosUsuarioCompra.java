package com.proyecto.servicios.model.dto.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa los datos requeridos de la compra que se obtienen y que serán cifrados en la petición:
 * id producto, idcliente, telefono, referencia, hora local, monto, upc (random string de 10 dígitos) y unidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatosUsuarioCompra {

    private String idProducto;
    private Integer idCliente;
    private String telefono;
    private String referencia;
    private String horaLocal;
    private BigDecimal monto;
    private String upc;
    private String unidad;
}
