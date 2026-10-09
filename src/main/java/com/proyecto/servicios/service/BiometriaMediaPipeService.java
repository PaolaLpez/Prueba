package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;

public interface BiometriaMediaPipeService {
    void registrarBiometria(BiometriaRequest request);
    double[] obtenerEmbeddingDescifrado(Integer clienteId);
    boolean verificarBiometria(Integer clienteId, double[] embeddingPrueba);
}
