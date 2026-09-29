package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.seguridad.DatosBiometricos;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.DatosBiometricosRepository;
import com.proyecto.servicios.service.BiometriaMediaPipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BiometriaMediaPipeServiceImpl implements BiometriaMediaPipeService {

    private final DatosBiometricosRepository datosBiometricosRepository;
    private final ClienteRepository clienteRepository;

    private static final String SECRET_KEY = "MediaPipeSecretKeyKeyKeySecretKey"; // 32 bytes AES-256

    public BiometriaMediaPipeServiceImpl(DatosBiometricosRepository datosBiometricosRepository,
                                         ClienteRepository clienteRepository) {
        this.datosBiometricosRepository = datosBiometricosRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void registrarBiometria(BiometriaRequest request) {
        log.info("Registrando biometría facial MediaPipe para clienteId={}", request.getClienteId());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id: " + request.getClienteId()));

        if (request.getFaceEmbedding() == null || request.getFaceEmbedding().length == 0) {
            throw new ReglaNegocioException("El vector de embedding MediaPipe no puede estar vacío");
        }

        String embeddingText = Arrays.stream(request.getFaceEmbedding())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        String cifrado = cifrarAES(embeddingText);

        DatosBiometricos biometricos = datosBiometricosRepository.findByClienteId(cliente.getId())
                .orElseGet(DatosBiometricos::new);

        biometricos.setCliente(cliente);
        biometricos.setTipoBiometrico("MEDIAPIPE_FACE_EMBEDDING");
        biometricos.setFaceEmbeddingCifrado(cifrado);

        datosBiometricosRepository.save(biometricos);
        log.info("Biometría facial MediaPipe cifrada y guardada correctamente");
    }

    @Override
    public double[] obtenerEmbeddingDescifrado(Integer clienteId) {
        DatosBiometricos biometricos = datosBiometricosRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existen datos biométricos para el cliente con id: " + clienteId));

        String descifrado = descifrarAES(biometricos.getFaceEmbeddingCifrado());
        return Arrays.stream(descifrado.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }

    @Override
    public boolean verificarBiometria(Integer clienteId, double[] embeddingPrueba) {
        double[] embeddingGuardado = obtenerEmbeddingDescifrado(clienteId);

        if (embeddingGuardado.length != embeddingPrueba.length) {
            return false;
        }

        double similitud = calcularSimilitudCoseno(embeddingGuardado, embeddingPrueba);
        log.info("Similitud cosmica MediaPipe calculada: {}", similitud);
        return similitud >= 0.85; // Umbral de coincidencia facial
    }

    private String cifrarAES(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error al cifrar datos biométricos: {}", e.getMessage());
            throw new ReglaNegocioException("Error al cifrar información biométrica");
        }
    }

    private String descifrarAES(String encryptedData) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al descifrar datos biométricos: {}", e.getMessage());
            throw new ReglaNegocioException("Error al descifrar información biométrica");
        }
    }

    private double calcularSimilitudCoseno(double[] vecA, double[] vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += Math.pow(vecA[i], 2);
            normB += Math.pow(vecB[i], 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
