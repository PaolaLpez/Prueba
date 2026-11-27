package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.seguridad.DatosBiometricos;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.DatosBiometricosRepository;
import com.proyecto.servicios.service.Impl.BiometriaMediaPipeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiometriaMediaPipeServiceImplTest {

    @Mock
    private DatosBiometricosRepository datosBiometricosRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private BiometriaMediaPipeServiceImpl biometriaService;

    private Cliente clienteTest;

    @BeforeEach
    void setUp() {
        clienteTest = new Cliente();
        clienteTest.setId(1);
        clienteTest.setNombre("Paola");
        clienteTest.setActivo(true);
    }

    @Test
    @DisplayName("Debe registrar biometría exitosamente cifrando el vector")
    void testRegistrarBiometria_Exitoso() {
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(1);
        request.setFaceEmbedding(new double[]{0.5, 0.5, 0.5});

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        when(datosBiometricosRepository.findByClienteId(1)).thenReturn(Optional.empty());

        biometriaService.registrarBiometria(request);

        verify(datosBiometricosRepository, times(1)).save(any(DatosBiometricos.class));
    }

    @Test
    @DisplayName("Debe fallar si cliente no existe al registrar biometría")
    void testRegistrarBiometria_ClienteNoExiste() {
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(999);
        request.setFaceEmbedding(new double[]{0.1, 0.2});

        when(clienteRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> biometriaService.registrarBiometria(request));
    }

    @Test
    @DisplayName("Debe fallar si el vector de embedding es nulo o vacío")
    void testRegistrarBiometria_VectorVacio() {
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(1);
        request.setFaceEmbedding(new double[]{});

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));

        assertThrows(ReglaNegocioException.class, () -> biometriaService.registrarBiometria(request));
    }

    @Test
    @DisplayName("Debe fallar al obtener embedding si no existen datos biométricos para el cliente")
    void testObtenerEmbedding_NoExiste() {
        when(datosBiometricosRepository.findByClienteId(1)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> biometriaService.obtenerEmbeddingDescifrado(1));
    }

    @Test
    @DisplayName("Debe verificar biometría facial exitosamente y validar umbrales")
    void testVerificarBiometria() {
        // First register to get a valid ciphertext
        final DatosBiometricos[] captured = new DatosBiometricos[1];
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        when(datosBiometricosRepository.findByClienteId(1)).thenReturn(Optional.empty());
        doAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            return captured[0];
        }).when(datosBiometricosRepository).save(any(DatosBiometricos.class));

        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(1);
        request.setFaceEmbedding(new double[]{0.5, 0.5, 0.5});
        biometriaService.registrarBiometria(request);

        assertNotNull(captured[0]);
        when(datosBiometricosRepository.findByClienteId(1)).thenReturn(Optional.of(captured[0]));

        // Descifrado
        double[] descifrado = biometriaService.obtenerEmbeddingDescifrado(1);
        assertEquals(3, descifrado.length);
        assertEquals(0.5, descifrado[0]);

        // Verificación exitosa con vector idéntico (similitud = 1.0 >= 0.85)
        assertTrue(biometriaService.verificarBiometria(1, new double[]{0.5, 0.5, 0.5}));

        // Longitud diferente (retorna false)
        assertFalse(biometriaService.verificarBiometria(1, new double[]{0.5, 0.5}));

        // Similitud baja (retorna false)
        assertFalse(biometriaService.verificarBiometria(1, new double[]{-0.5, -0.5, 0.5}));
    }
}
