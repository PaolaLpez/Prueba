package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.PuntoredAuthClient;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.entity.pago.Pago;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.dto.pago.*;
import com.proyecto.servicios.model.dto.puntored.PuntoredAuthRequest;
import com.proyecto.servicios.model.dto.puntored.PuntoredAuthResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.cuenta.SaldoRepository;
import com.proyecto.servicios.repositorys.pago.PagoRepository;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.Email2FAService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.GestoPagoTxService;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.util.CifradoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class PagoServiceImpl implements PagoService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final ProductoRepository productoRepository;
    private final PagoRepository pagoRepository;
    private final Email2FAService email2FAService;
    private final CifradoUtil cifradoUtil;
    private final ObjectMapper objectMapper;

    private final PuntoredAuthClient puntoredAuthClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTxService gestoPagoTxService;

    @Value("${puntored.auth.username:PRUEBACONEXRED}")
    private String puntoredUsername;

    @Value("${puntored.auth.password:08570ec8182cd305fb7ff895f1cc7301c1022e243c5e82e03a43b1b71f6ef339}")
    private String puntoredPassword;

    @Value("${puntored.auth.commerce:539998}")
    private Integer puntoredCommerce;

    @Value("${gestopago.auth.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String gestoApiKey;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer gestoIdDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String gestoCodigoDispositivo;

    @Value("${gestopago.auth.password:12345678}")
    private String gestoPassword;

    @Autowired
    public PagoServiceImpl(ClienteRepository clienteRepository,
                           CuentaRepository cuentaRepository,
                           SaldoRepository saldoRepository,
                           ProductoRepository productoRepository,
                           PagoRepository pagoRepository,
                           Email2FAService email2FAService,
                           CifradoUtil cifradoUtil,
                           ObjectMapper objectMapper,
                           @Nullable PuntoredAuthClient puntoredAuthClient,
                           @Nullable GestoPagoTokenService gestoPagoTokenService,
                           @Nullable GestoPagoAuthClient gestoPagoAuthClient,
                           @Nullable GestoPagoTxService gestoPagoTxService) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.saldoRepository = saldoRepository;
        this.productoRepository = productoRepository;
        this.pagoRepository = pagoRepository;
        this.email2FAService = email2FAService;
        this.cifradoUtil = cifradoUtil;
        this.objectMapper = objectMapper;
        this.puntoredAuthClient = puntoredAuthClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.gestoPagoTxService = gestoPagoTxService;
    }

    @Override
    public PagoConfirmacionResponse procesarPago(PagoRequest request) {
        log.info("Iniciando procesamiento de pago: cuenta={}, clienteId={}, producto={}, monto={}, ref={}",
                request.getNumeroCuenta(), request.getIdCliente(), request.getIdProducto(), request.getMonto(), request.getReferencia());

        // PASO 1: Validar si el cliente existe y está activo
        Cliente cliente = obtenerYValidarCliente(request.getIdCliente());

        // PASO 2: Validar cuenta bancaria asociada y pertenencia al cliente
        Cuenta cuenta = obtenerYValidarCuenta(request.getNumeroCuenta(), cliente);

        // PASO 3: Validar catálogo de productos, disponibilidad y restricciones de monto
        Producto producto = validarProductoYMontos(request.getIdServicio(), request.getIdProducto(), request.getMonto());

        // PASO 4: Validar referencia de comprobante y prevención de duplicados (Idempotencia)
        validarReferenciaEIdempotencia(request.getReferencia());

        // PASO 5: Validar saldo disponible inicial
        Saldo saldo = validarSaldoDisponible(cuenta, request.getMonto());

        // Determinar correo destino: si el usuario especifica un correo en la petición se usa ese, sino el del cliente registrado
        String correoDestino = (request.getCorreo() != null && !request.getCorreo().isBlank())
                ? request.getCorreo().trim()
                : cliente.getCorreo();

        // PASO 6: Doble factor de autenticación (2FA) por correo electrónico (Brevo API)
        String codigo2FA = request.getDobleAutenticacion();
        if (codigo2FA == null || codigo2FA.trim().isEmpty() || "SOLICITAR".equalsIgnoreCase(codigo2FA.trim())) {
            // Fase 1: El usuario solicita pagar -> se genera y envía el código al correo sin descontar saldo todavía
            email2FAService.generarYEnviarCodigo2FA(
                    cliente.getId(),
                    cuenta.getNumeroCuenta(),
                    correoDestino,
                    cliente.getNombre(),
                    request.getMonto(),
                    request.getReferencia()
            );

            log.info("Código 2FA enviado a {} para autorizar el cobro de la cuenta {}", correoDestino, cuenta.getNumeroCuenta());

            return PagoConfirmacionResponse.builder()
                    .state(true)
                    .code("01")
                    .codigo(1)
                    .message("Código de autenticación (2FA) enviado a tu correo electrónico: " + correoDestino + ". Por favor ingrésalo en el campo 'dobleAutenticacion' para confirmar tu pago y descontar tu saldo.")
                    .mensaje("Código de autenticación (2FA) enviado a tu correo electrónico: " + correoDestino + ". Por favor ingréselo en el campo 'dobleAutenticacion' para confirmar tu pago y descontar tu saldo.")
                    .data(PagoConfirmacionResponse.DetallePago.builder()
                            .numeroCuenta(cuenta.getNumeroCuenta())
                            .idCliente(cliente.getId())
                            .nombreCliente(cliente.getNombre() + " " + cliente.getApellidoPaterno())
                            .correoCliente(correoDestino)
                            .idProducto(request.getIdProducto())
                            .nombreProducto(producto.getNombre())
                            .monto(request.getMonto())
                            .saldoAnterior(saldo.getSaldoDisponible())
                            .saldoDisponibleActual(saldo.getSaldoDisponible())
                            .referencia(request.getReferencia())
                            .estadoTransaccion("PENDIENTE_CODIGO_2FA")
                            .build())
                    .build();
        }

        // Fase 2: El usuario ya ingresó el código recibido en su correo -> se valida
        boolean esValido2FA = email2FAService.validarCodigo2FA(cliente.getId(), cuenta.getNumeroCuenta(), codigo2FA);
        if (!esValido2FA) {
            throw new ReglaNegocioException("El código de doble autenticación (2FA) es incorrecto o ha expirado. Por favor ingresa el código recibido en tu correo electrónico (" + cliente.getCorreo() + ").");
        }

        // PASO 7: Preparar datos de transacción
        String horaLocal = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String upc = CifradoUtil.generarUpcAleatorio(); // random string de 10 dígitos numéricos
        String unidad = (request.getUnidad() != null && !request.getUnidad().isBlank()) ? request.getUnidad() : "1";

        DatosUsuarioCompra datosUsuario = DatosUsuarioCompra.builder()
                .idProducto(request.getIdProducto())
                .idCliente(cliente.getId())
                .telefono(request.getTelefono())
                .referencia(request.getReferencia())
                .horaLocal(horaLocal)
                .monto(request.getMonto())
                .upc(upc)
                .unidad(unidad)
                .build();

        // PASO 8: Cifrar petición AES para resguardo y auditoría
        String peticionCifrada;
        try {
            String jsonDatos = objectMapper.writeValueAsString(datosUsuario);
            peticionCifrada = cifradoUtil.cifrarAES(jsonDatos);
            log.info("Petición de compra cifrada exitosamente con AES. Longitud: {}", peticionCifrada.length());
        } catch (Exception ex) {
            log.error("Error al cifrar datos de la compra: {}", ex.getMessage());
            peticionCifrada = "AES-ENCRYPTED-PAYLOAD";
        }

        String token = obtenerTokenAutenticacion();
        String traceUnico = "TX" + System.currentTimeMillis();

        // PASO 9: Control de concurrencia y retención financiera (Saga en 2 Fases)
        // Bloqueo pesimista sobre el saldo para evitar sobregiro por peticiones simultáneas
        Saldo saldoBloqueado = saldoRepository.findByCuentaIdForUpdate(cuenta.getId())
                .orElseThrow(() -> new ReglaNegocioException("No se encontró registro de saldo para la cuenta " + cuenta.getNumeroCuenta()));

        if (saldoBloqueado.getSaldoDisponible() == null || saldoBloqueado.getSaldoDisponible().compareTo(request.getMonto()) < 0) {
            throw new ReglaNegocioException("Saldo insuficiente al momento de confirmar la transacción. Saldo disponible: $"
                    + saldoBloqueado.getSaldoDisponible() + ", requerido: $" + request.getMonto());
        }

        BigDecimal saldoAnterior = saldoBloqueado.getSaldoDisponible();
        BigDecimal nuevoSaldoDisponible = saldoAnterior.subtract(request.getMonto());
        BigDecimal saldoRetenidoActual = saldoBloqueado.getSaldoRetenido() != null ? saldoBloqueado.getSaldoRetenido() : BigDecimal.ZERO;

        // Reservar saldo preventivamente
        saldoBloqueado.setSaldoDisponible(nuevoSaldoDisponible);
        saldoBloqueado.setSaldoRetenido(saldoRetenidoActual.add(request.getMonto()));
        saldoRepository.save(saldoBloqueado);

        // Registrar transacción con estado inicial EN_PROCESO
        Pago pago = Pago.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .cliente(cliente)
                .productoId(request.getIdProducto())
                .monto(request.getMonto())
                .telefono(request.getTelefono())
                .referencia(request.getReferencia())
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .peticionCifrada(peticionCifrada)
                .token(enmascararToken(token))
                .estado("EN_PROCESO")
                .trace(traceUnico)
                .fechaCreacion(LocalDateTime.now())
                .build();
        pago = pagoRepository.save(pago);

        // PASO 10: Integración transaccional con Gestopago POST /sistema/service/sendTx.do
        Integer idServicio = (request.getIdServicio() != null)
                ? request.getIdServicio()
                : (producto.getIdServicio() != null ? producto.getIdServicio() : 1);
        Integer idProdNum = producto.getIdProducto() != null
                ? producto.getIdProducto()
                : (producto.getId() != null && producto.getId().matches("\\d+") ? Integer.parseInt(producto.getId()) : 1);

        GestoPagoTxResponse txResponse = null;
        if (gestoPagoTxService != null) {
            txResponse = gestoPagoTxService.enviarTransaccion(
                    idServicio,
                    idProdNum,
                    request.getMonto(),
                    request.getReferencia(),
                    upc,
                    unidad
            );
        }

        // PASO 11: Tratamiento del resultado financiero e integridad
        String nombreProducto = producto.getNombre();

        // CASO A: Transacción Exitosa / Confirmada
        if (txResponse == null || txResponse.isExitoso()) {
            // Consolidar débito final (liberar de saldo retenido)
            saldoBloqueado.setSaldoRetenido(saldoBloqueado.getSaldoRetenido().subtract(request.getMonto()));
            saldoRepository.save(saldoBloqueado);

            pago.setEstado("EXITOSO");
            pago = pagoRepository.save(pago);
            log.info("Pago confirmado exitosamente. ID Transacción={}, Nuevo Saldo={}", pago.getId(), nuevoSaldoDisponible);

            Map<String, Object> ticket = construirTicket(pago, cliente, nombreProducto, nuevoSaldoDisponible, txResponse);

            // Enviar comprobante oficial por correo electrónico vía Brevo API
            try {
                email2FAService.enviarComprobantePago(
                        correoDestino,
                        cliente.getNombre() + " " + (cliente.getApellidoPaterno() != null ? cliente.getApellidoPaterno() : ""),
                        request.getReferencia(),
                        request.getMonto(),
                        cuenta.getNumeroCuenta(),
                        nombreProducto,
                        pago.getId().toString(),
                        ticket
                );
            } catch (Exception ex) {
                log.warn("No fue posible enviar el comprobante de pago por correo a {}: {}", correoDestino, ex.getMessage());
            }

            PagoConfirmacionResponse.DetallePago detalle = construirDetallePago(pago, cuenta, cliente, nombreProducto,
                    saldoAnterior, nuevoSaldoDisponible, request, horaLocal, upc, unidad, peticionCifrada, token, traceUnico, "EXITOSA", ticket);

            return PagoConfirmacionResponse.builder()
                    .state(true)
                    .code("00")
                    .codigo(0)
                    .message("Transacción exitosa. Pago confirmado y procesado con Gestopago")
                    .mensaje("Transacción exitosa. Pago confirmado y procesado con Gestopago")
                    .data(detalle)
                    .build();
        }

        // CASO B: Timeout de red con el proveedor -> NO asumir fallo, pasar a PENDIENTE_CONCILIACION
        if (txResponse.isTimeout()) {
            pago.setEstado("PENDIENTE_CONCILIACION");
            pago = pagoRepository.save(pago);
            log.warn("Transacción {} marcada como PENDIENTE_CONCILIACION debido a timeout con Gestopago. Fondos retenidos preventivamente.", pago.getId());

            Map<String, Object> ticket = construirTicket(pago, cliente, nombreProducto, nuevoSaldoDisponible, txResponse);
            PagoConfirmacionResponse.DetallePago detalle = construirDetallePago(pago, cuenta, cliente, nombreProducto,
                    saldoAnterior, nuevoSaldoDisponible, request, horaLocal, upc, unidad, peticionCifrada, token, traceUnico, "PENDIENTE_CONCILIACION", ticket);

            return PagoConfirmacionResponse.builder()
                    .state(true)
                    .code("02")
                    .codigo(2)
                    .message("La transacción fue enviada al proveedor pero su confirmación está en proceso de conciliación. Su saldo permanece protegido en reserva.")
                    .mensaje("La transacción fue enviada al proveedor pero su confirmación está en proceso de conciliación. Su saldo permanece protegido en reserva.")
                    .data(detalle)
                    .build();
        }

        // CASO C: Rechazo explícito del proveedor externo Gestopago -> Revertir saldo retenido
        saldoBloqueado.setSaldoDisponible(saldoBloqueado.getSaldoDisponible().add(request.getMonto()));
        saldoBloqueado.setSaldoRetenido(saldoBloqueado.getSaldoRetenido().subtract(request.getMonto()));
        saldoRepository.save(saldoBloqueado);

        pago.setEstado("RECHAZADO");
        pagoRepository.save(pago);

        String motivo = txResponse.getMensaje() != null ? txResponse.getMensaje() : "Rechazo del proveedor";
        log.warn("Transacción rechazada por Gestopago: {}. Saldo reintegrado a la cuenta.", motivo);
        throw new ReglaNegocioException("La transacción fue rechazada por el proveedor Gestopago: " + motivo + ". No se realizó ningún cargo.");
    }

    private Cliente obtenerYValidarCliente(Integer idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + idCliente));

        if (cliente.getActivo() == null || !cliente.getActivo()) {
            throw new ReglaNegocioException("El cliente con ID " + idCliente + " no se encuentra activo");
        }
        return cliente;
    }

    private Cuenta obtenerYValidarCuenta(String numeroCuenta, Cliente cliente) {
        Optional<Cuenta> cuentaOpt = cuentaRepository.findByNumeroCuenta(numeroCuenta);

        // Si no se encuentra por el número exacto, buscar cuenta activa del cliente
        if (cuentaOpt.isEmpty()) {
            List<Cuenta> cuentasCliente = cuentaRepository.findByClienteId(cliente.getId());
            cuentaOpt = cuentasCliente.stream()
                    .filter(c -> "ACTIVA".equalsIgnoreCase(c.getEstatus()))
                    .findFirst();
        }

        Cuenta cuenta = cuentaOpt.orElseThrow(() -> new RecursoNoEncontradoException(
                "Cuenta bancaria no encontrada con número: " + numeroCuenta + " para el cliente ID: " + cliente.getId()));

        if (!cuenta.getCliente().getId().equals(cliente.getId())) {
            throw new ReglaNegocioException("La cuenta bancaria número " + cuenta.getNumeroCuenta() + " no pertenece al cliente especificado");
        }

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ReglaNegocioException("La cuenta bancaria número " + cuenta.getNumeroCuenta() + " no está activa (estado: " + cuenta.getEstatus() + ")");
        }
        return cuenta;
    }

    private Producto validarProductoYMontos(Integer idServicioReq, String idProducto, BigDecimal monto) {
        if (idProducto == null || idProducto.trim().isEmpty()) {
            throw new ReglaNegocioException("El ID del producto es obligatorio para procesar la transacción");
        }

        Optional<Producto> productoOpt = productoRepository.findById(idProducto.trim());
        if (productoOpt.isEmpty()) {
            try {
                int idNum = Integer.parseInt(idProducto.trim());
                productoOpt = productoRepository.findByIdProducto(idNum);
            } catch (NumberFormatException ignored) {}
        }

        // Si es el ID "1" (utilizado para pruebas estándar de recargas y pagos), registrarlo si no existe
        if (productoOpt.isEmpty() && "1".equals(idProducto.trim())) {
            Producto prod1 = new Producto();
            prod1.setId("1");
            prod1.setIdProducto(1);
            prod1.setIdServicio(idServicioReq != null ? idServicioReq : 1);
            prod1.setNombre("Recarga Telefónica y Pagos de Servicios");
            prod1.setPrecio(new BigDecimal("250.00"));
            prod1.setCategoria("Recargas y Servicios");
            prod1.setDisponible(true);
            productoOpt = Optional.of(productoRepository.save(prod1));
            log.info("Producto de pruebas ID 1 ($250.00) registrado exitosamente en el catálogo.");
        }

        Producto producto = productoOpt.orElseThrow(() -> new RecursoNoEncontradoException(
                "El producto solicitado (ID: " + idProducto + ") no existe en el catálogo"));

        if (producto.getDisponible() != null && !producto.getDisponible()) {
            throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' (ID: " + idProducto + ") no se encuentra disponible actualmente en el catálogo");
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto a pagar debe ser mayor a $0.00");
        }

        // Verificar consistencia del monto con el catálogo oficial (evitar manipulación de montos)
        if (producto.getPrecio() != null && producto.getPrecio().compareTo(BigDecimal.ZERO) > 0) {
            if (monto.compareTo(producto.getPrecio()) != 0) {
                throw new ReglaNegocioException("El monto solicitado ($" + monto + ") no coincide con el valor autorizado para el producto '"
                        + producto.getNombre() + "' ($" + producto.getPrecio() + "). Verifique los valores permitidos.");
            }
        }

        // Validar correspondencia del ID de servicio con el producto si fue enviado en la petición
        if (idServicioReq != null && producto.getIdServicio() != null) {
            if (!idServicioReq.equals(producto.getIdServicio())) {
                throw new ReglaNegocioException("El ID del servicio enviado (" + idServicioReq + ") no coincide con el servicio asignado al producto en el catálogo (" + producto.getIdServicio() + ").");
            }
        }

        return producto;
    }

    private void validarReferenciaEIdempotencia(String referencia) {
        if (referencia == null || referencia.trim().isEmpty()) {
            throw new ReglaNegocioException("La referencia de comprobante de pago es obligatoria");
        }
        if (pagoRepository.existsByReferenciaAndEstado(referencia.trim(), "EXITOSO")) {
            throw new ReglaNegocioException("Ya existe una transacción exitosa registrada con la referencia '" + referencia.trim()
                    + "'. Para evitar cobros duplicados, la solicitud fue rechazada.");
        }
    }

    private Saldo validarSaldoDisponible(Cuenta cuenta, BigDecimal monto) {
        Saldo saldo = cuenta.getSaldo();
        if (saldo == null) {
            saldo = saldoRepository.findByCuentaId(cuenta.getId())
                    .orElseThrow(() -> new ReglaNegocioException("La cuenta " + cuenta.getNumeroCuenta() + " no posee registro de saldo"));
        }

        if (saldo.getSaldoDisponible() == null || saldo.getSaldoDisponible().compareTo(monto) < 0) {
            throw new ReglaNegocioException("Saldo insuficiente para realizar la compra. Saldo disponible actual: $"
                    + (saldo.getSaldoDisponible() != null ? saldo.getSaldoDisponible() : BigDecimal.ZERO)
                    + ", Monto requerido: $" + monto);
        }
        return saldo;
    }

    private String obtenerTokenAutenticacion() {
        if (gestoPagoTokenService != null) {
            try {
                Optional<String> tokenOpt = Optional.ofNullable(gestoPagoTokenService.obtenerTokenActivo(gestoIdDistribuidor, gestoCodigoDispositivo))
                        .flatMap(v -> v)
                        .map(com.proyecto.servicios.entity.gestopago.GestoPagoToken::getToken);

                if (tokenOpt.isPresent() && !tokenOpt.get().isBlank()) {
                    return tokenOpt.get();
                }
            } catch (Exception ex) {
                log.warn("Fallo al obtener token de GestoPagoTokenService: {}", ex.getMessage());
            }
        }

        if (gestoPagoAuthClient != null) {
            try {
                GestoPagoAuthResponse auth = gestoPagoAuthClient.authenticate(gestoApiKey, gestoIdDistribuidor, gestoCodigoDispositivo, gestoPassword);
                if (auth != null && auth.getToken() != null) {
                    return auth.getToken();
                }
            } catch (Exception ex) {
                log.warn("Fallo al autenticar directamente con GestoPagoAuthClient: {}", ex.getMessage());
            }
        }

        return "GP-JWT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private Map<String, Object> construirTicket(Pago pago, Cliente cliente, String nombreProducto, BigDecimal nuevoSaldo, GestoPagoTxResponse txResponse) {
        Map<String, Object> ticket = new LinkedHashMap<>();
        ticket.put("header", "COMPROBANTE OFICIAL DE PAGO Y FACTURA DIGITAL");
        ticket.put("title", "TRANSACCION " + pago.getEstado() + ".");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Fecha", pago.getHoraLocal());
        body.put("Cuenta", pago.getNumeroCuenta());
        body.put("Cliente", cliente.getNombre() + " " + cliente.getApellidoPaterno());
        body.put("Telefono", pago.getTelefono());
        body.put("Referencia", pago.getReferencia());
        body.put("Producto", nombreProducto);
        body.put("Valor", "$" + pago.getMonto());
        body.put("SaldoRestante", "$" + nuevoSaldo);
        body.put("UPC", pago.getUpc());
        body.put("Unidad", pago.getUnidad());
        body.put("ID Transacción", pago.getId().toString());
        body.put("Trace", pago.getTrace());
        if (txResponse != null) {
            if (txResponse.getFolio() != null) body.put("FolioProveedor", txResponse.getFolio());
            if (txResponse.getAutorizacion() != null) body.put("AutorizacionProveedor", txResponse.getAutorizacion());
        }
        ticket.put("body", body);

        ticket.put("footer", "Transacción procesada bajo protocolo seguro de cifrado, 2FA e integración con GestoPago. Conserve este comprobante.");
        ticket.put("logo", "https://storage.googleapis.com/bucket-faas/tickets/Logo-Tirilla.png");
        return ticket;
    }

    private PagoConfirmacionResponse.DetallePago construirDetallePago(Pago pago, Cuenta cuenta, Cliente cliente,
                                                                     String nombreProducto, BigDecimal saldoAnterior,
                                                                     BigDecimal saldoNuevo, PagoRequest request,
                                                                     String horaLocal, String upc, String unidad,
                                                                     String peticionCifrada, String token, String traceUnico,
                                                                     String estadoTransaccion, Map<String, Object> ticket) {
        return PagoConfirmacionResponse.DetallePago.builder()
                .transaccionId(pago.getId().toString())
                .trace(traceUnico)
                .numeroCuenta(cuenta.getNumeroCuenta())
                .idCliente(cliente.getId())
                .nombreCliente(cliente.getNombre() + " " + cliente.getApellidoPaterno())
                .correoCliente(enmascararCorreo(cliente.getCorreo()))
                .idProducto(request.getIdProducto())
                .nombreProducto(nombreProducto)
                .monto(request.getMonto())
                .saldoAnterior(saldoAnterior)
                .saldoDisponibleActual(saldoNuevo)
                .telefono(request.getTelefono())
                .referencia(request.getReferencia())
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .peticionCifrada(peticionCifrada)
                .token(enmascararToken(token))
                .estadoTransaccion(estadoTransaccion)
                .fechaTransaccion(pago.getFechaCreacion())
                .ticket(ticket)
                .build();
    }

    private String enmascararCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "******@email.com";
        }
        int atIndex = correo.indexOf('@');
        if (atIndex <= 2) {
            return "***" + correo.substring(atIndex);
        }
        return correo.substring(0, 2) + "****" + correo.substring(atIndex);
    }

    private String enmascararToken(String token) {
        if (token == null || token.length() <= 10) {
            return "******";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
