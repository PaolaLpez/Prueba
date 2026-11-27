package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;
import com.proyecto.servicios.model.dto.cuenta.TransferenciaRequest;
import com.proyecto.servicios.model.dto.cuenta.TransferenciaResponse;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.cuenta.SaldoRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;

    public CuentaServiceImpl(CuentaRepository cuentaRepository, SaldoRepository saldoRepository) {
        this.cuentaRepository = cuentaRepository;
        this.saldoRepository = saldoRepository;
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

    @Override
    @Transactional
    public TransferenciaResponse realizarTransferencia(TransferenciaRequest request) {
        log.info("Iniciando transferencia de cuenta {} a cuenta {} por monto {}",
                request.getNumeroCuentaOrigen(), request.getNumeroCuentaDestino(), request.getMonto());

        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto a transferir debe ser mayor a cero");
        }

        if (request.getNumeroCuentaOrigen().trim().equalsIgnoreCase(request.getNumeroCuentaDestino().trim())) {
            throw new ReglaNegocioException("La cuenta de origen y destino no pueden ser la misma");
        }

        Cuenta cuentaOrigen = cuentaRepository.findByNumeroCuenta(request.getNumeroCuentaOrigen().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta origen no encontrada: " + request.getNumeroCuentaOrigen()));

        Cuenta cuentaDestino = cuentaRepository.findByNumeroCuenta(request.getNumeroCuentaDestino().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta destino no encontrada: " + request.getNumeroCuentaDestino()));

        if (!"ACTIVA".equalsIgnoreCase(cuentaOrigen.getEstatus())) {
            throw new ReglaNegocioException("La cuenta de origen se encuentra inactiva");
        }

        if (!"ACTIVA".equalsIgnoreCase(cuentaDestino.getEstatus())) {
            throw new ReglaNegocioException("La cuenta de destino se encuentra inactiva o dada de baja");
        }

        Saldo saldoOrigen = saldoRepository.findByCuentaIdForUpdate(cuentaOrigen.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Saldo no encontrado para cuenta origen"));

        Saldo saldoDestino = saldoRepository.findByCuentaIdForUpdate(cuentaDestino.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Saldo no encontrado para cuenta destino"));

        if (saldoOrigen.getSaldoDisponible().compareTo(request.getMonto()) < 0) {
            throw new ReglaNegocioException("Saldo insuficiente en cuenta origen. Saldo disponible: $" + saldoOrigen.getSaldoDisponible());
        }

        // Debitar cuenta origen y acreditar cuenta destino
        saldoOrigen.setSaldoDisponible(saldoOrigen.getSaldoDisponible().subtract(request.getMonto()));
        saldoDestino.setSaldoDisponible(saldoDestino.getSaldoDisponible().add(request.getMonto()));

        saldoRepository.save(saldoOrigen);
        saldoRepository.save(saldoDestino);

        String folio = "TRF-" + (System.currentTimeMillis() % 90000000 + 10000000);
        String destinatario = (cuentaDestino.getCliente() != null && cuentaDestino.getCliente().getNombre() != null)
                ? (cuentaDestino.getCliente().getNombre() + " " + (cuentaDestino.getCliente().getApellidoPaterno() != null ? cuentaDestino.getCliente().getApellidoPaterno() : ""))
                : "Usuario GestoPago";

        log.info("Transferencia exitosa folio {}. Nuevo saldo origen: {}, destino: {}",
                folio, saldoOrigen.getSaldoDisponible(), saldoDestino.getSaldoDisponible());

        return TransferenciaResponse.builder()
                .folioTransferencia(folio)
                .numeroCuentaOrigen(cuentaOrigen.getNumeroCuenta())
                .numeroCuentaDestino(cuentaDestino.getNumeroCuenta())
                .nombreDestinatario(destinatario.trim())
                .monto(request.getMonto())
                .concepto(request.getConcepto() != null && !request.getConcepto().trim().isEmpty() ? request.getConcepto().trim() : "Transferencia Administrativa")
                .saldoNuevoOrigen(saldoOrigen.getSaldoDisponible())
                .saldoNuevoDestino(saldoDestino.getSaldoDisponible())
                .fechaOperacion(LocalDateTime.now())
                .estatus("EXITOSA")
                .mensaje("Transferencia bancaria aplicada en tiempo real")
                .build();
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
