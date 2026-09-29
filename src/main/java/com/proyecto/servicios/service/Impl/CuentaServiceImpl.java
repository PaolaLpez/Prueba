package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;

    public CuentaServiceImpl(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public CuentaResponse consultarPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cuenta bancaria por número: {}", numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria no encontrada con el número: " + numeroCuenta));

        return mapToResponse(cuenta);
    }

    @Override
    public List<CuentaResponse> consultarCuentasActivas() {
        log.info("Consultando todas las cuentas bancarias activas");
        return cuentaRepository.findByEstatus("ACTIVA")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal consultarSaldo(String numeroCuenta) {
        CuentaResponse cuenta = consultarPorNumeroCuenta(numeroCuenta);
        return cuenta.getSaldoDisponible();
    }

    private CuentaResponse mapToResponse(Cuenta cuenta) {
        return CuentaResponse.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .estatus(cuenta.getEstatus())
                .saldoDisponible(cuenta.getSaldo() != null ? cuenta.getSaldo().getSaldoDisponible() : BigDecimal.ZERO)
                .saldoRetenido(cuenta.getSaldo() != null ? cuenta.getSaldo().getSaldoRetenido() : BigDecimal.ZERO)
                .fechaApertura(cuenta.getFechaApertura())
                .fechaActualizacionSaldo(cuenta.getSaldo() != null ? cuenta.getSaldo().getFechaActualizacion() : null)
                .build();
    }
}
