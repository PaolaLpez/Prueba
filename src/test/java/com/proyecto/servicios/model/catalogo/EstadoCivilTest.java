package com.proyecto.servicios.model.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadoCivilTest {

    @Test
    @DisplayName("Debe validar y normalizar los valores del enum EstadoCivil")
    void testEstadoCivil() {
        assertTrue(EstadoCivil.esValido("SOLTERO"));
        assertTrue(EstadoCivil.esValido("casado"));
        assertTrue(EstadoCivil.esValido("UNION_LIBRE"));
        assertFalse(EstadoCivil.esValido("DESCONOCIDO"));
        assertFalse(EstadoCivil.esValido(null));
        assertFalse(EstadoCivil.esValido(""));

        assertEquals("UNION LIBRE", EstadoCivil.normalizar("union_libre"));
        assertEquals("SOLTERO", EstadoCivil.normalizar("soltero"));
        assertNull(EstadoCivil.normalizar(null));

        List<String> permitidos = EstadoCivil.valoresPermitidos();
        assertNotNull(permitidos);
        assertEquals(5, permitidos.size());
        assertTrue(permitidos.contains("SOLTERO"));
        assertEquals("SOLTERO", EstadoCivil.SOLTERO.getValor());
    }
}

