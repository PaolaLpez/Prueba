package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.entity.seguridad.UsuarioLogin;
import com.proyecto.servicios.exception.ClienteDuplicadoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.catalogo.EstadoCivil;
import com.proyecto.servicios.model.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteBusquedaRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.model.dto.cliente.DomicilioDto;
import com.proyecto.servicios.model.dto.cuenta.CuentaResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
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

        // 1. Validación estricta de campos no vacíos y longitud mínima de nombres (al menos 3 letras)
        validarCamposCliente(request.getNombre(), request.getApellidoPaterno(), request.getApellidoMaterno());
        validarCurpFormato(request.getCurp());
        validarRfcFormato(request.getRfc());
        validarCorreoFormato(request.getCorreo());
        validarTelefonoFormato(request.getTelefonoMovil());
        validarSexo(request.getSexo());
        validarNacionalidad(request.getNacionalidad());
        validarEstadoCivil(request.getEstadoCivil());
        validarPasswordSegura(request.getPassword());

        // 2. Regla de Negocio: Mayoría de edad (18 años o más)
        validarMayoriaDeEdad(request.getFechaNacimiento());

        // 3. Regla de Negocio: Unicidad de CURP, RFC, Correo, Teléfono y Nombre completo
        validarUnicidad(request.getNombre(), request.getApellidoPaterno(), request.getApellidoMaterno(), request.getFechaNacimiento(), request.getCurp(), request.getRfc(), request.getCorreo(), request.getTelefonoMovil());

        // 4. Crear entidad Cliente
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre().trim());
        cliente.setSegundoNombre(request.getSegundoNombre() != null && !request.getSegundoNombre().trim().isEmpty() ? request.getSegundoNombre().trim() : null);
        cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp().trim().toUpperCase());
        cliente.setRfc(request.getRfc().trim().toUpperCase());
        cliente.setSexo(request.getSexo().trim().toUpperCase());
        cliente.setNacionalidad(request.getNacionalidad().trim().toUpperCase());
        cliente.setEstadoCivil(EstadoCivil.normalizar(request.getEstadoCivil()));
        cliente.setCorreo(request.getCorreo().trim().toLowerCase());
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null && !request.getTelefonoAlternativo().trim().isEmpty() ? request.getTelefonoAlternativo().trim() : null);
        cliente.setOcupacion(request.getOcupacion() != null ? request.getOcupacion().trim() : null);
        cliente.setEmpresa(request.getEmpresa() != null ? request.getEmpresa().trim() : null);
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);

        // 5. Agregar Domicilio
        if (request.getDomicilio() != null) {
            Domicilio dom = mapToDomicilioEntity(request.getDomicilio());
            cliente.addDomicilio(dom);
        }

        // Guardar cliente
        Cliente clienteGuardado = clienteRepository.save(cliente);

        // 6. Creación Automática de Cuenta Bancaria (Número único, estatus ACTIVA, saldo inicial no negativo)
        BigDecimal saldoInicial = request.getSaldoInicial() != null ? request.getSaldoInicial() : new BigDecimal("500.00");
        Cuenta cuentaBancaria = crearCuentaBancariaAutomatica(clienteGuardado, saldoInicial);
        cuentaRepository.save(cuentaBancaria);

        // 7. Crear Credenciales de Login si se proporcionó contraseña
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            UsuarioLogin login = new UsuarioLogin();
            login.setCliente(clienteGuardado);
            login.setUsername(request.getCorreo().trim().toLowerCase());
            login.setPasswordHash(request.getPassword().trim()); // En producción utilizar PasswordEncoder
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
        validarCorreoFormato(correo);
        Cliente cliente = clienteRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con correo: " + correo.trim().toLowerCase()));
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

        if (inicio == null || fin == null) {
            throw new ReglaNegocioException("Las fechas de inicio y fin son obligatorias");
        }

        // 1. Permitir fechas a partir del año 2026 en adelante (no permitir años anteriores como 2025 o previos)
        if (inicio.getYear() < 2026) {
            throw new ReglaNegocioException("Solo se permiten consultas a partir del año 2026 en adelante. El año de inicio (" + inicio.getYear() + ") no es válido");
        }
        if (fin.getYear() < 2026) {
            throw new ReglaNegocioException("Solo se permiten consultas a partir del año 2026 en adelante. El año de fin (" + fin.getYear() + ") no es válido");
        }

        // 2. Validación cronológica: la fecha de inicio no puede ser posterior a la fecha de fin
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha de inicio (" + inicio + ") no puede ser posterior a la fecha de fin (" + fin + ")");
        }

        // 3. Validación de fechas futuras: días, meses o años que aún no han llegado
        LocalDate hoy = LocalDate.now();
        if (inicio.isAfter(hoy)) {
            throw new ReglaNegocioException("No existen registros disponibles para días, meses o años que aún no han transcurrido. La fecha de inicio (" + inicio + ") no puede ser posterior a la fecha actual (" + hoy + ")");
        }
        if (fin.isAfter(hoy)) {
            throw new ReglaNegocioException("No existen registros disponibles para días, meses o años que aún no han transcurrido. La fecha de fin (" + fin + ") no puede ser posterior a la fecha actual (" + hoy + ")");
        }

        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime finDt = fin.atTime(23, 59, 59);
        return clienteRepository.findByFechaCreacionBetween(inicioDt, finDt).stream()
                .map(c -> mapToResponse(c, cuentaRepository.findByClienteId(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> buscarClientesDinamico(ClienteBusquedaRequest request) {
        log.info("Ejecutando búsqueda dinámica combinada de clientes con filtros: {}", request);
        Specification<Cliente> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (request != null) {
                if (request.getId() != null && request.getId() > 0) {
                    predicates.add(cb.equal(root.get("id"), request.getId()));
                }
                if (request.getCurp() != null && !request.getCurp().trim().isEmpty()) {
                    predicates.add(cb.equal(cb.upper(root.get("curp")), request.getCurp().trim().toUpperCase()));
                }
                if (request.getRfc() != null && !request.getRfc().trim().isEmpty()) {
                    predicates.add(cb.equal(cb.upper(root.get("rfc")), request.getRfc().trim().toUpperCase()));
                }
                if (request.getCorreo() != null && !request.getCorreo().trim().isEmpty()) {
                    predicates.add(cb.equal(cb.lower(root.get("correo")), request.getCorreo().trim().toLowerCase()));
                }
                if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + request.getNombre().trim().toLowerCase() + "%"));
                }
                if (request.getApellidoPaterno() != null && !request.getApellidoPaterno().trim().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + request.getApellidoPaterno().trim().toLowerCase() + "%"));
                }
                if (request.getApellidoMaterno() != null && !request.getApellidoMaterno().trim().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("apellidoMaterno")), "%" + request.getApellidoMaterno().trim().toLowerCase() + "%"));
                }
                if (request.getTelefonoMovil() != null && !request.getTelefonoMovil().trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("telefonoMovil"), request.getTelefonoMovil().trim()));
                }
                if (request.getActivo() != null) {
                    predicates.add(cb.equal(root.get("activo"), request.getActivo()));
                }
                if (request.getFechaInicio() != null && request.getFechaFin() != null) {
                    if (request.getFechaInicio().isAfter(request.getFechaFin())) {
                        throw new ReglaNegocioException("La fecha de inicio no puede ser posterior a la fecha de fin");
                    }
                    LocalDateTime inicioDt = request.getFechaInicio().atStartOfDay();
                    LocalDateTime finDt = request.getFechaFin().atTime(23, 59, 59);
                    predicates.add(cb.between(root.get("fechaCreacion"), inicioDt, finDt));
                } else if (request.getFechaInicio() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), request.getFechaInicio().atStartOfDay()));
                } else if (request.getFechaFin() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), request.getFechaFin().atTime(23, 59, 59)));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return clienteRepository.findAll(spec).stream()
                .map(c -> mapToResponse(c, cuentaRepository.findByClienteId(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(ClienteActualizacionRequest request) {
        if (request == null || request.getId() == null) {
            throw new ReglaNegocioException("El ID del cliente es obligatorio para realizar la actualización");
        }
        return actualizarCliente(request.getId(), request);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Integer id, ClienteActualizacionRequest request) {
        log.info("Iniciando actualización de información para cliente con ID: {}", id);
        try {
            // 1. El RFC nunca debe venir nulo ni la CURP (validación estricta)
            if (request.getCurp() == null || request.getCurp().trim().isEmpty()) {
                throw new ReglaNegocioException("La CURP es obligatoria y nunca debe venir nula ni vacía");
            }
            if (request.getRfc() == null || request.getRfc().trim().isEmpty()) {
                throw new ReglaNegocioException("El RFC es obligatorio y nunca debe venir nulo ni vacío");
            }

            // 2. Validar cliente: si existe manda la información, si no no
            Optional<Cliente> existeCliente = clienteRepository.findById(id);
            if (!existeCliente.isPresent()) {
                log.warn("Cliente con ID {} no encontrado para actualizar", id);
                throw new RecursoNoEncontradoException("Cliente no encontrado con ID: " + id);
            }

            // 3. Crea una entidad de cliente para acceder a esos datos
            Cliente clienteActualiza = existeCliente.get();

            // Validar formato y unicidad de CURP
            validarCurpFormato(request.getCurp());
            String curpUpper = request.getCurp().trim().toUpperCase();
            if (!curpUpper.equalsIgnoreCase(clienteActualiza.getCurp())) {
                if (clienteRepository.existsByCurp(curpUpper)) {
                    throw new ClienteDuplicadoException("La CURP ya está registrada por otro cliente: " + request.getCurp());
                }
                clienteActualiza.setCurp(curpUpper);
            }

            // Validar formato y unicidad de RFC
            validarRfcFormato(request.getRfc());
            String rfcUpper = request.getRfc().trim().toUpperCase();
            if (!rfcUpper.equalsIgnoreCase(clienteActualiza.getRfc())) {
                if (clienteRepository.existsByRfc(rfcUpper)) {
                    throw new ClienteDuplicadoException("El RFC ya está registrado por otro cliente: " + request.getRfc());
                }
                clienteActualiza.setRfc(rfcUpper);
            }

            // Validación y actualización de los demás campos
            if (request.getNombre() != null) {
                validarTextoNoVacioYLongitud("nombre", request.getNombre(), 3);
                clienteActualiza.setNombre(request.getNombre().trim());
            }
            if (request.getSegundoNombre() != null) {
                if (!request.getSegundoNombre().trim().isEmpty() && request.getSegundoNombre().trim().length() < 3) {
                    throw new ReglaNegocioException("El segundo nombre debe tener al menos 3 caracteres si se proporciona");
                }
                clienteActualiza.setSegundoNombre(request.getSegundoNombre().trim().isEmpty() ? null : request.getSegundoNombre().trim());
            }
            if (request.getApellidoPaterno() != null) {
                validarTextoNoVacioYLongitud("apellido paterno", request.getApellidoPaterno(), 3);
                clienteActualiza.setApellidoPaterno(request.getApellidoPaterno().trim());
            }
            if (request.getApellidoMaterno() != null) {
                validarTextoNoVacioYLongitud("apellido materno", request.getApellidoMaterno(), 3);
                clienteActualiza.setApellidoMaterno(request.getApellidoMaterno().trim());
            }
            if (request.getFechaNacimiento() != null) {
                validarMayoriaDeEdad(request.getFechaNacimiento());
                clienteActualiza.setFechaNacimiento(request.getFechaNacimiento());
            }
            if (request.getSexo() != null) {
                validarSexo(request.getSexo());
                clienteActualiza.setSexo(request.getSexo().trim().toUpperCase());
            }
            if (request.getNacionalidad() != null) {
                validarNacionalidad(request.getNacionalidad());
                clienteActualiza.setNacionalidad(request.getNacionalidad().trim().toUpperCase());
            }
            if (request.getEstadoCivil() != null) {
                validarEstadoCivil(request.getEstadoCivil());
                clienteActualiza.setEstadoCivil(EstadoCivil.normalizar(request.getEstadoCivil()));
            }
            if (request.getCorreo() != null) {
                validarCorreoFormato(request.getCorreo());
                String correoLower = request.getCorreo().trim().toLowerCase();
                if (!correoLower.equalsIgnoreCase(clienteActualiza.getCorreo())) {
                    if (clienteRepository.existsByCorreo(correoLower)) {
                        throw new ClienteDuplicadoException("El correo ya está registrado por otro cliente: " + request.getCorreo());
                    }
                    clienteActualiza.setCorreo(correoLower);
                }
            }
            if (request.getTelefonoMovil() != null) {
                validarTelefonoFormato(request.getTelefonoMovil());
                clienteActualiza.setTelefonoMovil(request.getTelefonoMovil().trim());
            }
            if (request.getTelefonoAlternativo() != null) {
                if (!request.getTelefonoAlternativo().trim().isEmpty() && !request.getTelefonoAlternativo().trim().matches("^\\d{10}$")) {
                    throw new ReglaNegocioException("El teléfono alternativo debe contener exactamente 10 dígitos");
                }
                clienteActualiza.setTelefonoAlternativo(request.getTelefonoAlternativo().trim().isEmpty() ? null : request.getTelefonoAlternativo().trim());
            }
            if (request.getOcupacion() != null) clienteActualiza.setOcupacion(request.getOcupacion().trim());
            if (request.getEmpresa() != null) clienteActualiza.setEmpresa(request.getEmpresa().trim());
            if (request.getIngresoMensual() != null) {
                if (request.getIngresoMensual().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ReglaNegocioException("El ingreso mensual debe ser mayor a cero");
                }
                clienteActualiza.setIngresoMensual(request.getIngresoMensual());
            }

            if (request.getDomicilio() != null) {
                if (!clienteActualiza.getDomicilios().isEmpty()) {
                    Domicilio dom = clienteActualiza.getDomicilios().get(0);
                    actualizarDomicilio(dom, request.getDomicilio());
                } else {
                    clienteActualiza.addDomicilio(mapToDomicilioEntity(request.getDomicilio()));
                }
            }

            Cliente clienteGuardado = clienteRepository.save(clienteActualiza);
            log.info("Cliente con ID {} actualizado correctamente", id);
            return mapToResponse(clienteGuardado, cuentaRepository.findByClienteId(id));

        } catch (RecursoNoEncontradoException | ReglaNegocioException | ClienteDuplicadoException e) {
            log.error("Error al actualizar cliente con ID {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al actualizar cliente con ID {}: ", id, e);
            throw new ReglaNegocioException("Error al actualizar cliente: " + e.getMessage());
        }
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

    private void validarCamposCliente(String nombre, String apellidoPaterno, String apellidoMaterno) {
        validarTextoNoVacioYLongitud("nombre", nombre, 3);
        validarTextoNoVacioYLongitud("apellido paterno", apellidoPaterno, 3);
        validarTextoNoVacioYLongitud("apellido materno", apellidoMaterno, 3);
    }

    private void validarTextoNoVacioYLongitud(String campo, String valor, int longitudMinima) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ReglaNegocioException("El campo " + campo + " es obligatorio y no puede estar vacío");
        }
        if (valor.trim().length() < longitudMinima) {
            throw new ReglaNegocioException("El " + campo + " debe tener al menos " + longitudMinima + " letras (no se permiten nombres de dos letras)");
        }
        if (!valor.trim().matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{" + longitudMinima + ",50}$")) {
            throw new ReglaNegocioException("El " + campo + " solo debe contener letras y espacios (no se permiten números ni caracteres extraños)");
        }
    }

    private static final java.util.Set<String> CATALOGO_NACIONALIDADES = java.util.Set.of(
        "MEXICANA", "EXTRANJERA", "ESTADOUNIDENSE", "CANADIENSE", "ESPANIOLA", "COLOMBIANA", "ARGENTINA", "OTRA"
    );

    private void validarNacionalidad(String nacionalidad) {
        if (nacionalidad == null || nacionalidad.trim().isEmpty()) {
            throw new ReglaNegocioException("La nacionalidad es obligatoria y no puede estar vacía");
        }
        String nacUpper = nacionalidad.trim().toUpperCase();
        if (!CATALOGO_NACIONALIDADES.contains(nacUpper)) {
            throw new ReglaNegocioException("La nacionalidad '" + nacionalidad + "' es inválida. Opciones del catálogo permitidas: " + String.join(", ", CATALOGO_NACIONALIDADES));
        }
    }

    private void validarEstadoCivil(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.trim().isEmpty()) {
            throw new ReglaNegocioException("El estado civil es obligatorio y no puede estar vacío");
        }
        if (!EstadoCivil.esValido(estadoCivil)) {
            throw new ReglaNegocioException("El estado civil '" + estadoCivil + "' es inválido. Opciones del catálogo permitidas: " + String.join(", ", EstadoCivil.valoresPermitidos()));
        }
    }

    private void validarCurpFormato(String curp) {
        if (curp == null || curp.trim().isEmpty()) {
            throw new ReglaNegocioException("La CURP es obligatoria y no puede estar vacía");
        }
        String curpLimpia = curp.trim().toUpperCase();
        if (curpLimpia.length() != 18 || !curpLimpia.matches("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]\\d$")) {
            throw new ReglaNegocioException("El formato de la CURP es inválido (debe tener exactamente 18 caracteres en formato oficial)");
        }
    }

    private void validarRfcFormato(String rfc) {
        if (rfc == null || rfc.trim().isEmpty()) {
            throw new ReglaNegocioException("El RFC es obligatorio y no puede estar vacío");
        }
        String rfcLimpio = rfc.trim().toUpperCase();
        if (rfcLimpio.length() < 12 || rfcLimpio.length() > 13 || !rfcLimpio.matches("^[A-ZÑ&]{3,4}\\d{6}[A-V1-9][A-Z1-9][0-9A]$")) {
            throw new ReglaNegocioException("El formato del RFC es inválido (debe tener 12 o 13 caracteres en formato oficial)");
        }
    }

    private void validarCorreoFormato(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new ReglaNegocioException("El correo electrónico es obligatorio y no puede estar vacío");
        }
        if (!correo.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new ReglaNegocioException("El formato del correo electrónico es inválido (ejemplo: usuario@dominio.com)");
        }
    }

    private void validarTelefonoFormato(String telefono) {
        if (telefono == null || !telefono.trim().matches("^\\d{10}$")) {
            throw new ReglaNegocioException("El teléfono móvil debe contener exactamente 10 dígitos numéricos");
        }
    }

    private void validarSexo(String sexo) {
        if (sexo == null || sexo.trim().isEmpty()) {
            throw new ReglaNegocioException("El sexo es obligatorio y no puede estar vacío");
        }
        String sexoUpper = sexo.trim().toUpperCase();
        if (!sexoUpper.equals("MASCULINO") && !sexoUpper.equals("FEMENINO") && !sexoUpper.equals("OTRO")) {
            throw new ReglaNegocioException("El sexo debe ser exclusivamente MASCULINO, FEMENINO u OTRO");
        }
    }

    private void validarPasswordSegura(String password) {
        if (password == null || password.trim().isEmpty()) {
            return;
        }
        String pass = password.trim();
        if (pass.length() < 8) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!pass.matches(".*[A-Z].*")) {
            throw new ReglaNegocioException("La contraseña debe contener al menos una letra mayúscula");
        }
        if (!pass.matches(".*[a-z].*")) {
            throw new ReglaNegocioException("La contraseña debe contener al menos una letra minúscula");
        }
        if (!pass.matches(".*\\d.*")) {
            throw new ReglaNegocioException("La contraseña debe contener al menos un número");
        }
        if (!pass.matches(".*[^a-zA-Z0-9\\s].*")) {
            throw new ReglaNegocioException("La contraseña debe contener al menos un carácter especial (@, $, !, %, *, ?, &, #, etc.)");
        }
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

    private void validarUnicidad(String nombre, String apellidoPaterno, String apellidoMaterno, LocalDate fechaNacimiento,
                                 String curp, String rfc, String correo, String telefonoMovil) {
        List<String> duplicados = new java.util.ArrayList<>();

        String curpLimpia = curp.trim().toUpperCase();
        if (clienteRepository.existsByCurpIgnoreCase(curpLimpia)) {
            duplicados.add("la CURP: " + curpLimpia);
        }

        String rfcLimpio = rfc.trim().toUpperCase();
        if (clienteRepository.existsByRfcIgnoreCase(rfcLimpio)) {
            duplicados.add("el RFC: " + rfcLimpio);
        }

        String correoLimpio = correo.trim().toLowerCase();
        if (clienteRepository.existsByCorreoIgnoreCase(correoLimpio)) {
            duplicados.add("el correo: " + correoLimpio);
        } else if (usuarioLoginRepository.existsByUsername(correoLimpio)) {
            duplicados.add("el usuario de login: " + correoLimpio);
        }

        if (telefonoMovil != null && !telefonoMovil.trim().isEmpty()) {
            String telLimpio = telefonoMovil.trim();
            if (clienteRepository.existsByTelefonoMovil(telLimpio)) {
                duplicados.add("el teléfono móvil: " + telLimpio);
            }
        }

        String nom = nombre.trim();
        String apeP = apellidoPaterno.trim();
        String apeM = apellidoMaterno.trim();
        if (fechaNacimiento != null && clienteRepository.existsByNombreIgnoreCaseAndApellidoPaternoIgnoreCaseAndApellidoMaternoIgnoreCaseAndFechaNacimiento(nom, apeP, apeM, fechaNacimiento)) {
            duplicados.add("el mismo nombre (" + nom + " " + apeP + " " + apeM + ") y fecha de nacimiento");
        } else if (clienteRepository.existsByNombreIgnoreCaseAndApellidoPaternoIgnoreCaseAndApellidoMaternoIgnoreCase(nom, apeP, apeM)) {
            duplicados.add("el mismo nombre y apellidos (" + nom + " " + apeP + " " + apeM + ")");
        }

        if (!duplicados.isEmpty()) {
            throw new ClienteDuplicadoException("Ya existe un cliente registrado con " + String.join(", ", duplicados));
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
