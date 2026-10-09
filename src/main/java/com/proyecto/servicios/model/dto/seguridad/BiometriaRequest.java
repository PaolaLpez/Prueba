package com.proyecto.servicios.model.dto.seguridad;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BiometriaRequest {

    private Integer clienteId;

    @NotNull(message = "El vector de embedding facial de MediaPipe es obligatorio")
    private double[] faceEmbedding;

    private String tipoBiometrico;
}
