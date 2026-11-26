package com.proyecto.servicios.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CifradoUtilTest {

    private CifradoUtil cifradoUtil;

    @BeforeEach
    void setUp() {
        cifradoUtil = new CifradoUtil("PuntoredGestoPagoSecureSecretKey2026!");
    }

    @Test
    @DisplayName("Debe cifrar y descifrar texto plano con AES correctamente")
    void testCifrarYDescifrar() {
        String texto = "MensajeSecreto123";
        String cifrado = cifradoUtil.cifrarAES(texto);

        assertNotNull(cifrado);
        assertNotEquals(texto, cifrado);

        String descifrado = cifradoUtil.descifrarAES(cifrado);
        assertEquals(texto, descifrado);
    }

    @Test
    @DisplayName("Debe manejar nulos al cifrar o descifrar")
    void testManejoNulos() {
        assertNull(cifradoUtil.cifrarAES(null));
        assertNull(cifradoUtil.descifrarAES(null));
    }

    @Test
    @DisplayName("Debe fallar al descifrar texto corrupto o no válido")
    void testDescifrarInvalido() {
        assertThrows(IllegalStateException.class, () -> cifradoUtil.descifrarAES("TextoInvalidoNoBase64!#"));
    }

    @Test
    @DisplayName("Debe inicializar con secret nulo o vacío usando default")
    void testInicializacionDefault() {
        CifradoUtil utilDefault = new CifradoUtil(null);
        assertNotNull(utilDefault.cifrarAES("test"));

        CifradoUtil utilVacio = new CifradoUtil("   ");
        assertNotNull(utilVacio.cifrarAES("test"));
    }

    @Test
    @DisplayName("Debe generar UPC aleatorio de 10 dígitos")
    void testGenerarUpcAleatorio() {
        String upc = CifradoUtil.generarUpcAleatorio();

        assertNotNull(upc);
        assertEquals(10, upc.length());
        assertTrue(upc.matches("^\\d{10}$"));
    }
}
