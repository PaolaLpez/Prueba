package com.proyecto.servicios.entity.pago;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PagoTest {

    @Test
    @DisplayName("Debe verificar todos los campos, builder y lifecycle de Pago")
    public void testPagoEntity() {
        Cliente cliente = new Cliente();
        cliente.setId(1);

        Pago pago = new Pago();
        pago.setId(10L);
        pago.setNumeroCuenta("5730094736");
        pago.setCliente(cliente);
        pago.setProductoId("1");
        pago.setMonto(new BigDecimal("250.00"));
        pago.setTelefono("5512345678");
        pago.setReferencia("REF-100");
        pago.setHoraLocal("12:00:00");
        pago.setUpc("1234567890");
        pago.setUnidad("1");
        pago.setPeticionCifrada("AES-DATA");
        pago.setToken("TOKEN-123");
        pago.setEstado("EXITOSO");
        pago.setTrace("TR-1");
        pago.setFechaCreacion(LocalDateTime.now());

        assertEquals(10L, pago.getId());
        assertEquals("5730094736", pago.getNumeroCuenta());
        assertEquals(cliente, pago.getCliente());
        assertEquals("1", pago.getProductoId());
        assertEquals(new BigDecimal("250.00"), pago.getMonto());
        assertEquals("5512345678", pago.getTelefono());
        assertEquals("REF-100", pago.getReferencia());
        assertEquals("12:00:00", pago.getHoraLocal());
        assertEquals("1234567890", pago.getUpc());
        assertEquals("1", pago.getUnidad());
        assertEquals("AES-DATA", pago.getPeticionCifrada());
        assertEquals("TOKEN-123", pago.getToken());
        assertEquals("EXITOSO", pago.getEstado());
        assertEquals("TR-1", pago.getTrace());
        assertNotNull(pago.getFechaCreacion());

        // PrePersist lifecycle method (when null)
        Pago pagoPersist = new Pago();
        pagoPersist.onCreate();
        assertNotNull(pagoPersist.getFechaCreacion());
        assertEquals("EXITOSO", pagoPersist.getEstado());

        // PrePersist lifecycle method (when already set)
        Pago pagoSet = new Pago();
        LocalDateTime customTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        pagoSet.setFechaCreacion(customTime);
        pagoSet.setEstado("CANCELADO");
        pagoSet.onCreate();
        assertEquals(customTime, pagoSet.getFechaCreacion());
        assertEquals("CANCELADO", pagoSet.getEstado());

        // Builder
        Pago pagoBuilt = Pago.builder()
                .id(20L)
                .numeroCuenta("1234567890")
                .cliente(cliente)
                .productoId("2")
                .monto(new BigDecimal("50.00"))
                .telefono("5500000000")
                .referencia("REF-200")
                .horaLocal("10:00:00")
                .upc("0987654321")
                .unidad("1")
                .peticionCifrada("CIPHER")
                .token("TOK")
                .estado("PENDIENTE")
                .trace("TR-2")
                .fechaCreacion(LocalDateTime.now())
                .build();

        assertEquals(20L, pagoBuilt.getId());
        assertEquals("PENDIENTE", pagoBuilt.getEstado());

        // All-args constructor
        Pago allArgs = new Pago(30L, "111", cliente, "1", BigDecimal.TEN, "55", "REF", "11", "UPC", "1", "P", "T", "OK", "TR", LocalDateTime.now());
        assertEquals(30L, allArgs.getId());
    }
}
