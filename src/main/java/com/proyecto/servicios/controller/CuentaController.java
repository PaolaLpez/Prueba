package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;
    private final ClienteService clienteService;

    public CuentaController(CuentaService cuentaService, ClienteService clienteService) {
        this.cuentaService = cuentaService;
        this.clienteService = clienteService;
    }

    // 1. Consultar cuenta por número de cuenta
    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> consultarPorNumeroCuenta(@PathVariable("numeroCuenta") String numeroCuenta) {
        return new ResponseEntity<>(cuentaService.consultarPorNumeroCuenta(numeroCuenta), HttpStatus.OK);
    }

    // 2. Consultar cliente asociado a un número de cuenta
    @GetMapping(value = "/{numeroCuenta}/cliente", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarClientePorNumeroCuenta(@PathVariable("numeroCuenta") String numeroCuenta) {
        return new ResponseEntity<>(clienteService.consultarPorNumeroCuenta(numeroCuenta), HttpStatus.OK);
    }

    // 3. Consultar cuentas activas
    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CuentaResponse>> consultarCuentasActivas() {
        return new ResponseEntity<>(cuentaService.consultarCuentasActivas(), HttpStatus.OK);
    }

    // 4. Consultar saldo disponible de una cuenta
    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BigDecimal> consultarSaldo(@PathVariable("numeroCuenta") String numeroCuenta) {
        return new ResponseEntity<>(cuentaService.consultarSaldo(numeroCuenta), HttpStatus.OK);
    }
}
