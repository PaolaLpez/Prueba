package com.proyecto.servicios.entity.gestopago;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class GestoPagoTokenTest {

    @Test
    @DisplayName("Debe verificar getters, setters y ciclo de vida de GestoPagoToken")
    public void testGestoPagoToken() {
        GestoPagoToken token = new GestoPagoToken();
        token.setId(1);
        token.setIdDistribuidor(83);
        token.setCodigoDispositivo("GPS83-TPV-17");
        token.setToken("TOKEN-TEST");
        token.setTokenType("Bearer");
        token.setExpiresIn(3600L);
        token.setActivo(true);
        token.setFechaCreacion(LocalDateTime.now());
        token.setFechaActualizacion(LocalDateTime.now());

        assertEquals(1, token.getId());
        assertEquals(83, token.getIdDistribuidor());
        assertEquals("GPS83-TPV-17", token.getCodigoDispositivo());
        assertEquals("TOKEN-TEST", token.getToken());
        assertEquals("Bearer", token.getTokenType());
        assertEquals(3600L, token.getExpiresIn());
        assertTrue(token.getActivo());
        assertNotNull(token.getFechaCreacion());
        assertNotNull(token.getFechaActualizacion());

        // Lifecycle onCreate and onUpdate
        GestoPagoToken tokenLifecycle = new GestoPagoToken();
        tokenLifecycle.onCreate();
        assertNotNull(tokenLifecycle.getFechaCreacion());
        assertNotNull(tokenLifecycle.getFechaActualizacion());

        tokenLifecycle.onUpdate();
        assertNotNull(tokenLifecycle.getFechaActualizacion());
    }
}

