package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceImplTest {

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    @InjectMocks
    private GestoPagoTokenServiceImpl gestoPagoTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gestoPagoTokenService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(gestoPagoTokenService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gestoPagoTokenService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(gestoPagoTokenService, "password", "12345678");
    }

    @Test
    @DisplayName("Debe renovar token creando uno nuevo cuando no existe previo")
    void testRenovarToken_Nuevo() {
        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("token-test-123");

        GestoPagoToken nuevoToken = new GestoPagoToken();
        nuevoToken.setToken("token-test-123");

        when(gestoPagoAuthClient.authenticate(anyString(), anyInt(), anyString(), anyString())).thenReturn(authResponse);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());
        when(tokenMapper.toEntity(authResponse)).thenReturn(nuevoToken);

        gestoPagoTokenService.renovarToken();

        verify(tokenRepository, times(1)).save(nuevoToken);
        assertEquals(83, nuevoToken.getIdDistribuidor());
        assertEquals("GPS83-TPV-17", nuevoToken.getCodigoDispositivo());
        assertTrue(nuevoToken.getActivo());
    }

    @Test
    @DisplayName("Debe renovar token actualizando el existente")
    void testRenovarToken_Existente() {
        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("token-nuevo-456");

        GestoPagoToken tokenExistente = new GestoPagoToken();
        tokenExistente.setId(1);
        tokenExistente.setToken("token-viejo");

        when(gestoPagoAuthClient.authenticate(anyString(), anyInt(), anyString(), anyString())).thenReturn(authResponse);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenExistente));

        gestoPagoTokenService.renovarToken();

        verify(tokenMapper, times(1)).updateEntity(authResponse, tokenExistente);
        verify(tokenRepository, times(1)).save(tokenExistente);
    }

    @Test
    @DisplayName("Debe manejar respuesta nula o sin token de GestoPago sin lanzar excepción")
    void testRenovarToken_RespuestaNula() {
        when(gestoPagoAuthClient.authenticate(anyString(), anyInt(), anyString(), anyString())).thenReturn(null);

        gestoPagoTokenService.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe obtener token activo por distribuidor y dispositivo")
    void testObtenerTokenActivo() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-activo");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));

        Optional<GestoPagoToken> resultado = gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17");

        assertTrue(resultado.isPresent());
        assertEquals("token-activo", resultado.get().getToken());
    }

    @Test
    @DisplayName("Debe manejar excepción en cliente de autenticación sin fallar")
    void testRenovarToken_Excepcion() {
        when(gestoPagoAuthClient.authenticate(anyString(), anyInt(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Error de red GestoPago"));

        assertDoesNotThrow(() -> gestoPagoTokenService.renovarToken());
    }
}

