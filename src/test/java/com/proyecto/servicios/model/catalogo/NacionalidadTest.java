package com.proyecto.servicios.model.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NacionalidadTest {

    @Test
    @DisplayName("Debe validar y normalizar los valores del enum Nacionalidad")
    void testNacionalidad() {
        assertTrue(Nacionalidad.esValida("MEXICANA"));
        assertTrue(Nacionalidad.esValida("mexicana"));
        assertTrue(Nacionalidad.esValida("ESPAÑOLA"));
        assertTrue(Nacionalidad.esValida("ESPANIOLA"));
        assertFalse(Nacionalidad.esValida("MARCIANA"));
        assertFalse(Nacionalidad.esValida(null));
        assertFalse(Nacionalidad.esValida(""));

        assertEquals("MEXICANA", Nacionalidad.normalizar("mexicana"));
        assertEquals("ESPANIOLA", Nacionalidad.normalizar("ESPAÑOLA"));
        assertEquals("ESPANIOLA", Nacionalidad.normalizar("ESPANIOLA"));
        assertNull(Nacionalidad.normalizar(null));
        assertEquals("OTRO_VALOR", Nacionalidad.normalizar("OTRO_VALOR"));

        List<String> permitidos = Nacionalidad.valoresPermitidos();
        assertNotNull(permitidos);
        assertEquals(8, permitidos.size());
        assertTrue(permitidos.contains("MEXICANA"));
        assertEquals("MEXICANA", Nacionalidad.MEXICANA.getValor());
    }
}

