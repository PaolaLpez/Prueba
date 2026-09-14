package com.proyecto.servicios.model.dto.producto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO de respuesta procesada enviada por la API interna, omitiendo campos con valor nulo
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoResponse {

    private Integer codigo;
    private String mensaje;
    private List<ProductoDto> datos;
}
