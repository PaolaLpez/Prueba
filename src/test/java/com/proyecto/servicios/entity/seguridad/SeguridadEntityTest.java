package com.proyecto.servicios.entity.seguridad;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SeguridadEntityTest {

    @Test
    @DisplayName("Debe verificar getters, setters y ciclo de vida de UsuarioLogin y DatosBiometricos")
    public void testSeguridadEntities() {
        Cliente cliente = new Cliente();
        cliente.setId(1);

        UsuarioLogin login = new UsuarioLogin();
        login.setId(1);
        login.setCliente(cliente);
        login.setUsername("paola@gmail.com");
        login.setPasswordHash("hash123");
        login.setActivo(true);
        login.setUltimoAcceso(LocalDateTime.now());

        assertEquals(1, login.getId());
        assertEquals(cliente, login.getCliente());
        assertEquals("paola@gmail.com", login.getUsername());
        assertEquals("hash123", login.getPasswordHash());
        assertTrue(login.getActivo());
        assertNotNull(login.getUltimoAcceso());

        DatosBiometricos bio = new DatosBiometricos();
        bio.setId(1);
        bio.setCliente(cliente);
        bio.setTipoBiometrico("MEDIAPIPE_FACE_EMBEDDING");
        bio.setFaceEmbeddingCifrado("CIPHER-EMBEDDING");
        bio.setFechaRegistro(LocalDateTime.now());

        assertEquals(1, bio.getId());
        assertEquals(cliente, bio.getCliente());
        assertEquals("MEDIAPIPE_FACE_EMBEDDING", bio.getTipoBiometrico());
        assertEquals("CIPHER-EMBEDDING", bio.getFaceEmbeddingCifrado());
        assertNotNull(bio.getFechaRegistro());

        // Lifecycle onCreate (when null)
        DatosBiometricos bioLifecycle = new DatosBiometricos();
        bioLifecycle.onCreate();
        assertNotNull(bioLifecycle.getFechaRegistro());

        // Lifecycle onCreate (when already set)
        DatosBiometricos bioSet = new DatosBiometricos();
        LocalDateTime dt = LocalDateTime.of(2026, 1, 1, 0, 0);
        bioSet.setFechaRegistro(dt);
        bioSet.onCreate();
        assertEquals(dt, bioSet.getFechaRegistro());
    }
}
