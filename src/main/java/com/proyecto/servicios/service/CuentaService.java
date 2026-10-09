package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaService {

    CuentaResponse consultarPorNumeroCuenta(String numeroCuenta);
    List<CuentaResponse> consultarCuentasActivas();
    BigDecimal consultarSaldo(String numeroCuenta);
}
