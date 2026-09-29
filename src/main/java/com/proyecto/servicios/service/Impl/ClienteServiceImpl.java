package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.entity.seguridad.UsuarioLogin;
import com.proyecto.servicios.exception.ClienteDuplicadoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.model.dto.cliente.DomicilioDto;
import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioLoginRepository usuarioLoginRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              CuentaRepository cuentaRepository,
                              UsuarioLoginRepository usuarioLoginRepository) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioLoginRepository = usuarioLoginRepository;
    }

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteRegistroRequest request) {
        log.info("Iniciando registro de cliente con CURP: {} y Correo: {}", request.getCurp(), request.getCorreo());

        // 1. Regla de Negocio: Mayoría de edad (18 años o más)
        validarMayoriaDeEdad(request.getFechaNacimiento());

        // 2. Regla de Negocio: Unicidad de CURP, RFC y Correo
        validarUnicidad(request.getCurp(), request.getRfc(), request.getCorreo());

        // 3. Crear entidad Cliente
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp().toUpperCase());
        cliente.setRfc(request.getRfc().toUpperCase());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreo(request.getCorreo().toLowerCase());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);

        // 4. Agregar Domicilio
        if (request.getDomicilio() != null) {
            Domicilio dom = mapToDomicilioEntity(request.getDomicilio());
            cliente.addDomicilio(dom);
        }

        // Guardar cliente
        Cliente clienteGuardado = clienteRepository.save(cliente);

        // 5. Creación Automática de Cuenta Bancaria (Número único, estatus ACTIVA, saldo inicial no negativo)
        BigDecimal saldoInicial = request.getSaldoInicial() != null ? request.getSaldoInicial() : new BigDecimal("500.00");
        Cuenta cuentaBancaria = crearCuentaBancariaAutomatica(clienteGuardado, saldoInicial);
        cuentaRepository.save(cuentaBancaria);

        // 6. Crear Credenciales de Login si se proporcionó contraseña
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            UsuarioLogin login = new UsuarioLogin();
            login.setCliente(clienteGuardado);
            login.setUsername(request.getCorreo().toLowerCase());
            login.setPasswordHash(request.getPassword()); // En producción utilizar PasswordEncoder
            login.setActivo(true);
            usuarioLoginRepository.save(login);
        }

        log.info("Cliente registrado exitosamente con ID: {} y Número de Cuenta: {}", clienteGuardado.getId(), cuentaBancaria.getNumeroCuenta());
        return mapToResponse(clienteGuardado, List.of(cuentaBancaria));
    }

    @Override
    public List<ClienteResponse> consultarTodosClientes() {
        log.info("Consultando todos los clientes registrados");
        return clienteRepository.findAll().stream()
                .map(c -> mapToResponse(c, cuentaRepository.findByClienteId(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public ClienteResponse consultarPorId(Integer id) {
        log.info("Consultando cliente por ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + id));
        return mapToResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()));
    }

    @Override
    public ClienteResponse consultarPorCurp(String curp) {
        log.info("Consultando cliente por CURP: {}", curp);
        Cliente cliente = clienteRepository.findByCurp(curp.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con CURP: " + curp));
        return mapToResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()));
    }

    @Override
    public ClienteResponse consultarPorRfc(String rfc) {
        log.info("Consultando cliente por RFC: {}", rfc);
        Cliente cliente = clienteRepository.findByRfc(rfc.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con RFC: " + rfc));
        return mapToResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()));
    }

    @Override
    public ClienteResponse consultarPorCorreo(String correo) {
        log.info("Consultando cliente por correo: {}", correo);
        Cliente cliente = clienteRepository.findByCorreo(correo.toLowerCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con correo: " + correo));
        return mapToResponse(cliente, cuentaRepository.findByClienteId(cliente.getId()));
    }

    @Override
    public ClienteResponse consultarPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cliente por número de cuenta: {}", numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con número: " + numeroCuenta));
        return mapToResponse(cuenta.getCliente(), List.of(cuenta));
    }

    @Override
    public List<ClienteResponse> consultarClientesActivos() {
        log.info("Consultando clientes activos");
        return clienteRepository.findByActivoTrue().stream()
                .map(c -> mapToResponse(c, cuentaRepository.findByClienteId(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> consultarClientesPorRangoFechas(LocalDate inicio, LocalDate fin) {
        log.info("Consultando clientes registrados entre {} y {}", inicio, fin);
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime finDt = fin.atTime(23, 59, 59);
        return clienteRepository.findByFechaCreacionBetween(inicioDt, finDt).stream()
                .map(c -> mapToResponse(c, cuentaRepository.findByClienteId(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Integer id, ClienteActualizacionRequest request) {
        log.info("Actualizando información de cliente con ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + id));

        // Modificación permitida: datos personales, contacto, domicilio, laboral
        if (request.getNombre() != null) cliente.setNombre(request.getNombre());
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(request.getSegundoNombre());
        if (request.getApellidoPaterno() != null) cliente.setApellidoPaterno(request.getApellidoPaterno());
        if (request.getApellidoMaterno() != null) cliente.setApellidoMaterno(request.getApellidoMaterno());
        if (request.getFechaNacimiento() != null) {
            validarMayoriaDeEdad(request.getFechaNacimiento());
            cliente.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getSexo() != null) cliente.setSexo(request.getSexo());
        if (request.getNacionalidad() != null) cliente.setNacionalidad(request.getNacionalidad());
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(request.getEstadoCivil());

        if (request.getCorreo() != null && !request.getCorreo().equalsIgnoreCase(cliente.getCorreo())) {
            if (clienteRepository.existsByCorreo(request.getCorreo().toLowerCase())) {
                throw new ClienteDuplicadoException("El correo ya está registrado por otro cliente: " + request.getCorreo());
            }
            cliente.setCorreo(request.getCorreo().toLowerCase());
        }
        if (request.getTelefonoMovil() != null) cliente.setTelefonoMovil(request.getTelefonoMovil());
        if (request.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        if (request.getOcupacion() != null) cliente.setOcupacion(request.getOcupacion());
        if (request.getEmpresa() != null) cliente.setEmpresa(request.getEmpresa());
        if (request.getIngresoMensual() != null) cliente.setIngresoMensual(request.getIngresoMensual());

        if (request.getDomicilio() != null) {
            if (!cliente.getDomicilios().isEmpty()) {
                Domicilio dom = cliente.getDomicilios().get(0);
                actualizarDomicilio(dom, request.getDomicilio());
            } else {
                cliente.addDomicilio(mapToDomicilioEntity(request.getDomicilio()));
            }
        }

        Cliente clienteActualizado = clienteRepository.save(cliente);
        log.info("Cliente con ID {} actualizado correctamente", id);
        return mapToResponse(clienteActualizado, cuentaRepository.findByClienteId(id));
    }

    @Override
    @Transactional
    public void bajaLogicaCliente(Integer id) {
        log.info("Ejecutando baja lógica para cliente con ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + id));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        // Desactivar también las cuentas asociadas
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        for (Cuenta c : cuentas) {
            c.setEstatus("INACTIVA");
            cuentaRepository.save(c);
        }

        log.info("Baja lógica completada para cliente ID {} y sus cuentas asociadas", id);
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException("La fecha de nacimiento es obligatoria");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 años o más). Edad actual: " + edad + " años");
        }
    }

    private void validarUnicidad(String curp, String rfc, String correo) {
        if (clienteRepository.existsByCurp(curp.toUpperCase())) {
            throw new ClienteDuplicadoException("Ya existe un cliente registrado con la CURP: " + curp);
        }
        if (clienteRepository.existsByRfc(rfc.toUpperCase())) {
            throw new ClienteDuplicadoException("Ya existe un cliente registrado con el RFC: " + rfc);
        }
        if (clienteRepository.existsByCorreo(correo.toLowerCase())) {
            throw new ClienteDuplicadoException("Ya existe un cliente registrado con el correo: " + correo);
        }
    }

    private Cuenta crearCuentaBancariaAutomatica(Cliente cliente, BigDecimal saldoInicial) {
        String numeroCuenta;
        Random random = new Random();
        do {
            long num = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            numeroCuenta = String.valueOf(num);
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setEstatus("ACTIVA");

        Saldo saldo = new Saldo();
        saldo.setSaldoDisponible(saldoInicial);
        saldo.setSaldoRetenido(BigDecimal.ZERO);

        cuenta.setSaldo(saldo);
        return cuenta;
    }

    private Domicilio mapToDomicilioEntity(DomicilioDto dto) {
        Domicilio dom = new Domicilio();
        dom.setCalle(dto.getCalle());
        dom.setNumeroExterior(dto.getNumeroExterior());
        dom.setNumeroInterior(dto.getNumeroInterior());
        dom.setColonia(dto.getColonia());
        dom.setMunicipio(dto.getMunicipio());
        dom.setEstado(dto.getEstado());
        dom.setCodigoPostal(dto.getCodigoPostal());
        dom.setPais(dto.getPais());
        return dom;
    }

    private void actualizarDomicilio(Domicilio dom, DomicilioDto dto) {
        if (dto.getCalle() != null) dom.setCalle(dto.getCalle());
        if (dto.getNumeroExterior() != null) dom.setNumeroExterior(dto.getNumeroExterior());
        if (dto.getNumeroInterior() != null) dom.setNumeroInterior(dto.getNumeroInterior());
        if (dto.getColonia() != null) dom.setColonia(dto.getColonia());
        if (dto.getMunicipio() != null) dom.setMunicipio(dto.getMunicipio());
        if (dto.getEstado() != null) dom.setEstado(dto.getEstado());
        if (dto.getCodigoPostal() != null) dom.setCodigoPostal(dto.getCodigoPostal());
        if (dto.getPais() != null) dom.setPais(dto.getPais());
    }

    private ClienteResponse mapToResponse(Cliente cliente, List<Cuenta> cuentas) {
        List<DomicilioDto> domicilioDtos = cliente.getDomicilios() != null ?
                cliente.getDomicilios().stream().map(d -> DomicilioDto.builder()
                        .calle(d.getCalle())
                        .numeroExterior(d.getNumeroExterior())
                        .numeroInterior(d.getNumeroInterior())
                        .colonia(d.getColonia())
                        .municipio(d.getMunicipio())
                        .estado(d.getEstado())
                        .codigoPostal(d.getCodigoPostal())
                        .pais(d.getPais())
                        .build()).collect(Collectors.toList()) : Collections.emptyList();

        List<CuentaResponse> cuentaDtos = cuentas != null ?
                cuentas.stream().map(c -> CuentaResponse.builder()
                        .id(c.getId())
                        .numeroCuenta(c.getNumeroCuenta())
                        .estatus(c.getEstatus())
                        .saldoDisponible(c.getSaldo() != null ? c.getSaldo().getSaldoDisponible() : BigDecimal.ZERO)
                        .saldoRetenido(c.getSaldo() != null ? c.getSaldo().getSaldoRetenido() : BigDecimal.ZERO)
                        .fechaApertura(c.getFechaApertura())
                        .fechaActualizacionSaldo(c.getSaldo() != null ? c.getSaldo().getFechaActualizacion() : null)
                        .build()).collect(Collectors.toList()) : Collections.emptyList();

        return ClienteResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                .correo(cliente.getCorreo())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .ocupacion(cliente.getOcupacion())
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual())
                .activo(cliente.getActivo())
                .fechaCreacion(cliente.getFechaCreacion())
                .domicilios(domicilioDtos)
                .cuentas(cuentaDtos)
                .build();
    }
}
