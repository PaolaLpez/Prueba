package com.proyecto.servicios.entity.cuenta;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CuentaEntityTest {

    @Test
    @DisplayName("Debe verificar todos los campos, relaciones y ciclo de vida de Cuenta y Saldo")
    public void testCuentaYSaldo() {
        Cliente cliente = new Cliente();
        cliente.setId(1);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(5);
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta("5730094736");
        cuenta.setEstatus("ACTIVA");
        cuenta.setFechaApertura(LocalDateTime.now());

        Saldo saldo = new Saldo();
        saldo.setId(5);
        saldo.setSaldoDisponible(new BigDecimal("1000.00"));
        saldo.setSaldoRetenido(BigDecimal.ZERO);
        saldo.setFechaActualizacion(LocalDateTime.now());

        cuenta.setSaldo(saldo);

        assertEquals(5, cuenta.getId());
        assertEquals(cliente, cuenta.getCliente());
        assertEquals("5730094736", cuenta.getNumeroCuenta());
        assertEquals("ACTIVA", cuenta.getEstatus());
        assertNotNull(cuenta.getFechaApertura());
        assertEquals(saldo, cuenta.getSaldo());
        assertEquals(cuenta, saldo.getCuenta());
        assertEquals(new BigDecimal("1000.00"), saldo.getSaldoDisponible());
        assertEquals(BigDecimal.ZERO, saldo.getSaldoRetenido());

        // Null saldo
        cuenta.setSaldo(null);
        assertNull(cuenta.getSaldo());

        // Lifecycle onCreate on Cuenta (when null)
        Cuenta cPersist = new Cuenta();
        cPersist.setEstatus(null);
        cPersist.onCreate();
        assertNotNull(cPersist.getFechaApertura());
        assertEquals("ACTIVA", cPersist.getEstatus());

        // Lifecycle onCreate on Cuenta (when already set)
        Cuenta cSet = new Cuenta();
        LocalDateTime dt = LocalDateTime.of(2026, 1, 1, 0, 0);
        cSet.setFechaApertura(dt);
        cSet.setEstatus("BLOQUEADA");
        cSet.onCreate();
        assertEquals(dt, cSet.getFechaApertura());
        assertEquals("BLOQUEADA", cSet.getEstatus());

        // Lifecycle onSave on Saldo (when null)
        Saldo sPersist = new Saldo();
        sPersist.setSaldoRetenido(null);
        sPersist.onSave();
        assertNotNull(sPersist.getFechaActualizacion());
        assertEquals(BigDecimal.ZERO, sPersist.getSaldoRetenido());

        // Lifecycle onSave on Saldo (when already set)
        Saldo sSet = new Saldo();
        sSet.setSaldoRetenido(new BigDecimal("500.00"));
        sSet.onSave();
        assertEquals(new BigDecimal("500.00"), sSet.getSaldoRetenido());
    }
}
