package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaControllerTest {

    @Mock
    private CuentaService cuentaService;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private CuentaController cuentaController;

    @Test
    @DisplayName("Debe consultar cuenta por número de cuenta")
    void testConsultarPorNumeroCuenta() {
        CuentaResponse response = CuentaResponse.builder().numeroCuenta("12345").build();
        when(cuentaService.consultarPorNumeroCuenta("12345")).thenReturn(response);

        ResponseEntity<CuentaResponse> result = cuentaController.consultarPorNumeroCuenta("12345");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("12345", result.getBody().getNumeroCuenta());
    }

    @Test
    @DisplayName("Debe consultar cliente por número de cuenta")
    void testConsultarClientePorNumeroCuenta() {
        ClienteResponse cliente = ClienteResponse.builder().id(1).nombre("Paola").build();
        when(clienteService.consultarPorNumeroCuenta("12345")).thenReturn(cliente);

        ResponseEntity<ClienteResponse> result = cuentaController.consultarClientePorNumeroCuenta("12345");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Paola", result.getBody().getNombre());
    }

    @Test
    @DisplayName("Debe consultar cuentas activas")
    void testConsultarCuentasActivas() {
        CuentaResponse c = CuentaResponse.builder().numeroCuenta("12345").estatus("ACTIVA").build();
        when(cuentaService.consultarCuentasActivas()).thenReturn(List.of(c));

        ResponseEntity<List<CuentaResponse>> result = cuentaController.consultarCuentasActivas();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("Debe consultar saldo disponible")
    void testConsultarSaldo() {
        when(cuentaService.consultarSaldo("12345")).thenReturn(new BigDecimal("1500.00"));

        ResponseEntity<BigDecimal> result = cuentaController.consultarSaldo("12345");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("1500.00"), result.getBody());
    }
}

