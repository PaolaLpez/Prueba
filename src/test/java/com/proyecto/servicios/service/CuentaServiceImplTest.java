package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private Cuenta cuentaTest;
    private Saldo saldoTest;

    @BeforeEach
    void setUp() {
        cuentaTest = new Cuenta();
        cuentaTest.setId(1);
        cuentaTest.setNumeroCuenta("1234567890");
        cuentaTest.setEstatus("ACTIVA");
        cuentaTest.setFechaApertura(LocalDateTime.now());

        saldoTest = new Saldo();
        saldoTest.setId(1);
        saldoTest.setSaldoDisponible(new BigDecimal("5000.00"));
        saldoTest.setSaldoRetenido(BigDecimal.ZERO);
        saldoTest.setFechaActualizacion(LocalDateTime.now());
        cuentaTest.setSaldo(saldoTest);
    }

    @Test
    @DisplayName("Debe consultar cuenta por número exitosamente con saldo")
    void testConsultarPorNumeroCuenta_Exitoso() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaTest));

        CuentaResponse response = cuentaService.consultarPorNumeroCuenta("1234567890");

        assertNotNull(response);
        assertEquals("1234567890", response.getNumeroCuenta());
        assertEquals("ACTIVA", response.getEstatus());
        assertEquals(new BigDecimal("5000.00"), response.getSaldoDisponible());
        verify(cuentaRepository, times(1)).findByNumeroCuenta("1234567890");
    }

    @Test
    @DisplayName("Debe consultar cuenta por número cuando no tiene saldo inicial (saldo null)")
    void testConsultarPorNumeroCuenta_SinSaldo() {
        cuentaTest.setSaldo(null);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaTest));

        CuentaResponse response = cuentaService.consultarPorNumeroCuenta("1234567890");

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getSaldoDisponible());
        assertEquals(BigDecimal.ZERO, response.getSaldoRetenido());
        assertNull(response.getFechaActualizacionSaldo());
    }

    @Test
    @DisplayName("Debe lanzar RecursoNoEncontradoException cuando la cuenta no existe")
    void testConsultarPorNumeroCuenta_NoEncontrada() {
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.consultarPorNumeroCuenta("9999999999"));
    }

    @Test
    @DisplayName("Debe consultar lista de cuentas activas")
    void testConsultarCuentasActivas() {
        when(cuentaRepository.findByEstatus("ACTIVA")).thenReturn(List.of(cuentaTest));

        List<CuentaResponse> activas = cuentaService.consultarCuentasActivas();

        assertNotNull(activas);
        assertEquals(1, activas.size());
        assertEquals("1234567890", activas.get(0).getNumeroCuenta());
        verify(cuentaRepository, times(1)).findByEstatus("ACTIVA");
    }

    @Test
    @DisplayName("Debe consultar saldo disponible de la cuenta")
    void testConsultarSaldo() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaTest));

        BigDecimal saldo = cuentaService.consultarSaldo("1234567890");

        assertEquals(new BigDecimal("5000.00"), saldo);
    }
}

