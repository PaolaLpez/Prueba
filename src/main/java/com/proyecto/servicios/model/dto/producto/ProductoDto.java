package com.proyecto.servicios.model.dto.producto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO que representa un producto del servicio externo, omitiendo atributos nulos
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoDto {

    private String id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private String categoria;
    private Boolean disponible;
    private Integer idServicio;
    private Integer idProducto;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;
    private Boolean showAyuda;
    private String tipoReferencia;
    private String leyenda;

    @JsonProperty("servicio")
    public String getServicio() {
        return categoria != null ? categoria : "";
    }

    @JsonProperty("servicio")
    public void setServicio(String servicio) {
        if (this.categoria == null || this.categoria.isBlank()) {
            this.categoria = servicio;
        }
    }

    @JsonProperty("producto")
    public String getProducto() {
        return nombre != null ? nombre : "";
    }

    @JsonProperty("producto")
    public void setProducto(String producto) {
        if (this.nombre == null || this.nombre.isBlank()) {
            this.nombre = producto;
        }
    }
}
