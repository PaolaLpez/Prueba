package com.proyecto.servicios.entity.producto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ProductoEntityTest {

    @Test
    @DisplayName("Debe verificar getters y setters de Producto")
    public void testProducto() {
        Producto p = new Producto();
        p.setId("100");
        p.setNombre("Recarga");
        p.setPrecio(new BigDecimal("100.00"));
        p.setCategoria("Telefonia");
        p.setDisponible(true);
        p.setIdServicio(1);
        p.setIdProducto(1);
        p.setIdCatTipoServicio(2);
        p.setTipoFront(1);
        p.setHasDigitoVerificador(false);
        p.setShowAyuda(true);
        p.setTipoReferencia("NUMERO");

        assertEquals("100", p.getId());
        assertEquals("Recarga", p.getNombre());
        assertEquals(new BigDecimal("100.00"), p.getPrecio());
        assertEquals("Telefonia", p.getCategoria());
        assertTrue(p.getDisponible());
        assertEquals(1, p.getIdServicio());
        assertEquals(1, p.getIdProducto());
        assertEquals(2, p.getIdCatTipoServicio());
        assertEquals(1, p.getTipoFront());
        assertFalse(p.getHasDigitoVerificador());
        assertTrue(p.getShowAyuda());
        assertEquals("NUMERO", p.getTipoReferencia());
    }
}

