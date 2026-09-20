package com.proyecto.servicios.entity.producto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    private String nombre;
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
}