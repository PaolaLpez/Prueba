package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.entity.pago.Pago;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.dto.pago.PagoConfirmacionResponse;
import com.proyecto.servicios.model.dto.pago.PagoRequest;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.cuenta.SaldoRepository;
import com.proyecto.servicios.repositorys.pago.PagoRepository;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.util.CifradoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private Email2FAService email2FAService;

    @Mock
    private CifradoUtil cifradoUtil;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GestoPagoTxService gestoPagoTxService;

    @InjectMocks
    private PagoServiceImpl pagoService;

    private Cliente clienteTest;
    private Cuenta cuentaTest;
    private Saldo saldoTest;
    private Producto productoTest;

    @BeforeEach
    void setUp() {
        clienteTest = new Cliente();
        clienteTest.setId(3);
        clienteTest.setNombre("Paola");
        clienteTest.setApellidoPaterno("Lopez");
        clienteTest.setCorreo("lopezpao869@gmail.com");
        clienteTest.setTelefonoMovil("5512345678");
        clienteTest.setActivo(true);

        cuentaTest = new Cuenta();
        cuentaTest.setId(3);
        cuentaTest.setNumeroCuenta("5730094736");
        cuentaTest.setEstatus("ACTIVA");
        cuentaTest.setCliente(clienteTest);

        saldoTest = new Saldo();
        saldoTest.setId(3);
        saldoTest.setCuenta(cuentaTest);
        saldoTest.setSaldoDisponible(new BigDecimal("1000.00"));
        saldoTest.setSaldoRetenido(BigDecimal.ZERO);
        cuentaTest.setSaldo(saldoTest);

        productoTest = new Producto();
        productoTest.setId("1");
        productoTest.setIdProducto(1);
        productoTest.setIdServicio(1);
        productoTest.setNombre("Recarga Telefónica y Pagos de Servicios");
        productoTest.setPrecio(new BigDecimal("250.00"));
        productoTest.setDisponible(true);

        lenient().when(productoRepository.findById("1")).thenReturn(Optional.of(productoTest));
    }

    @Test
    @DisplayName("Fase 1 (Desafío 2FA): Debe despachar código al correo sin alterar saldo cuando dobleAutenticacion es null")
    void testProcesarPagoFase1_Despacho2FA() {
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .nombreProducto("Recarga Telefónica y Pagos de Servicios")
                .monto(new BigDecimal("250.00"))
                .dobleAutenticacion(null)
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(3)).thenReturn(List.of(cuentaTest));
        when(productoRepository.findAll()).thenReturn(List.of(productoTest));
        when(pagoRepository.existsByReferenciaAndEstado(anyString(), eq("EXITOSO"))).thenReturn(false);

        PagoConfirmacionResponse response = pagoService.procesarPago(request);

        assertNotNull(response);
        assertTrue(response.isState());
        assertEquals("01", response.getCode());
        assertEquals("PENDIENTE_CODIGO_2FA", response.getData().getEstadoTransaccion());
        verify(email2FAService, times(1)).generarYEnviarCodigo2FA(eq(3), eq("5730094736"), eq("lopezpao869@gmail.com"), anyString(), any(), anyString());
        // El saldo no debe haber sido tocado
        verify(saldoRepository, never()).findByCuentaIdForUpdate(anyInt());
        verify(gestoPagoTxService, never()).enviarTransaccion(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Fase 2: Con 2FA válido y fondos suficientes, debe reservar saldo, consumir GestoPago sendTx.do y confirmar")
    void testProcesarPagoFase2_Exitoso() {
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .nombreProducto("Recarga Telefónica y Pagos de Servicios")
                .monto(new BigDecimal("250.00"))
                .dobleAutenticacion("123456")
                .referencia("REF-PAOLA-001")
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(3)).thenReturn(List.of(cuentaTest));
        when(productoRepository.findAll()).thenReturn(List.of(productoTest));
        when(pagoRepository.existsByReferenciaAndEstado("REF-PAOLA-001", "EXITOSO")).thenReturn(false);
        when(email2FAService.validarCodigo2FA(3, "5730094736", "123456")).thenReturn(true);

        when(saldoRepository.findByCuentaIdForUpdate(3)).thenReturn(Optional.of(saldoTest));

        Pago pagoMock = Pago.builder()
                .id(100L)
                .numeroCuenta("5730094736")
                .cliente(clienteTest)
                .monto(new BigDecimal("250.00"))
                .referencia("REF-PAOLA-001")
                .estado("EN_PROCESO")
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoMock);

        GestoPagoTxResponse txOk = GestoPagoTxResponse.builder()
                .exitoso(true)
                .codigo(0)
                .folio("GP-112233")
                .autorizacion("AUTH-9988")
                .mensaje("Operación autorizada")
                .build();
        when(gestoPagoTxService.enviarTransaccion(eq(1), eq(1), eq(new BigDecimal("250.00")), eq("REF-PAOLA-001"), any(), any()))
                .thenReturn(txOk);

        PagoConfirmacionResponse response = pagoService.procesarPago(request);

        assertNotNull(response);
        assertTrue(response.isState());
        assertEquals("00", response.getCode());
        assertEquals("EXITOSA", response.getData().getEstadoTransaccion());
        // Verificar bloqueo pesimista y actualización de saldo
        verify(saldoRepository, atLeastOnce()).findByCuentaIdForUpdate(3);
        verify(saldoRepository, atLeastOnce()).save(saldoTest);
        verify(gestoPagoTxService, times(1)).enviarTransaccion(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe rechazar la transacción si el cliente no existe en base de datos")
    void testRechazarClienteInexistente() {
        PagoRequest request = PagoRequest.builder()
                .correo("noexiste@correo.com")
                .monto(new BigDecimal("250.00"))
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("noexiste@correo.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreo("noexiste@correo.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> pagoService.procesarPago(request));
    }

    @Test
    @DisplayName("Debe rechazar si el cliente se encuentra inactivo")
    void testRechazarClienteInactivo() {
        clienteTest.setActivo(false);
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .monto(new BigDecimal("250.00"))
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("inactiva"));
    }

    @Test
    @DisplayName("Debe rechazar si la referencia ya fue ejecutada (Anti-duplicados e idempotencia)")
    void testRechazarReferenciaDuplicada() {
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .referencia("REF-DUPLICADA-999")
                .monto(new BigDecimal("250.00"))
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(3)).thenReturn(List.of(cuentaTest));
        when(productoRepository.findAll()).thenReturn(List.of(productoTest));
        when(pagoRepository.existsByReferenciaAndEstado("REF-DUPLICADA-999", "EXITOSO")).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("cobros duplicados"));
    }

    @Test
    @DisplayName("Debe rechazar si el monto es manipulado y no coincide con el catálogo oficial")
    void testRechazarMontoManipulado() {
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .nombreProducto("Recarga Telefónica y Pagos de Servicios")
                .monto(new BigDecimal("10.00")) // Precio real es 250.00
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(3)).thenReturn(List.of(cuentaTest));
        when(productoRepository.findAll()).thenReturn(List.of(productoTest));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("no coincide con el valor autorizado"));
    }

    @Test
    @DisplayName("Debe rechazar si el saldo disponible es insuficiente")
    void testRechazarSaldoInsuficiente() {
        saldoTest.setSaldoDisponible(new BigDecimal("50.00")); // Requerido 250.00
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .nombreProducto("Recarga Telefónica y Pagos de Servicios")
                .monto(new BigDecimal("250.00"))
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(3)).thenReturn(List.of(cuentaTest));
        when(productoRepository.findAll()).thenReturn(List.of(productoTest));
        when(pagoRepository.existsByReferenciaAndEstado(anyString(), eq("EXITOSO"))).thenReturn(false);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
    }

    @Test
    @DisplayName("Timeout con Gestopago: NO asumir fallo, marcar PENDIENTE_CONCILIACION y retener saldo en reserva")
    void testTimeoutGestopago_ProtegeSaldo() {
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .nombreProducto("Recarga Telefónica y Pagos de Servicios")
                .monto(new BigDecimal("250.00"))
                .dobleAutenticacion("123456")
                .referencia("REF-PAOLA-TIMEOUT")
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(3)).thenReturn(List.of(cuentaTest));
        when(productoRepository.findAll()).thenReturn(List.of(productoTest));
        when(pagoRepository.existsByReferenciaAndEstado("REF-PAOLA-TIMEOUT", "EXITOSO")).thenReturn(false);
        when(email2FAService.validarCodigo2FA(3, "5730094736", "123456")).thenReturn(true);
        when(saldoRepository.findByCuentaIdForUpdate(3)).thenReturn(Optional.of(saldoTest));

        Pago pagoMock = Pago.builder()
                .id(101L)
                .numeroCuenta("5730094736")
                .cliente(clienteTest)
                .monto(new BigDecimal("250.00"))
                .referencia("REF-PAOLA-TIMEOUT")
                .estado("EN_PROCESO")
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoMock);

        GestoPagoTxResponse txTimeout = GestoPagoTxResponse.builder()
                .exitoso(false)
                .timeout(true)
                .codigo(504)
                .mensaje("Tiempo de espera agotado con el proveedor externo")
                .build();
        when(gestoPagoTxService.enviarTransaccion(any(), any(), any(), any(), any(), any())).thenReturn(txTimeout);

        PagoConfirmacionResponse response = pagoService.procesarPago(request);

        assertNotNull(response);
        assertEquals("02", response.getCode());
        assertEquals("PENDIENTE_CONCILIACION", response.getData().getEstadoTransaccion());
        // El dinero queda retenido preventivamente, no se devuelve ni se confirma a ciegas
        assertEquals(new BigDecimal("250.00"), saldoTest.getSaldoRetenido());
    }

    @Test
    @DisplayName("Rechazo de Gestopago: Debe revertir automáticamente el saldo retenido a disponible")
    void testRechazoGestopago_RevierteSaldo() {
        PagoRequest request = PagoRequest.builder()
                .correo("lopezpao869@gmail.com")
                .nombreProducto("Recarga Telefónica y Pagos de Servicios")
                .monto(new BigDecimal("250.00"))
                .dobleAutenticacion("123456")
                .referencia("REF-PAOLA-RECHAZO")
                .build();

        when(clienteRepository.findByCorreoIgnoreCase("lopezpao869@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(3)).thenReturn(List.of(cuentaTest));
        when(productoRepository.findAll()).thenReturn(List.of(productoTest));
        when(pagoRepository.existsByReferenciaAndEstado("REF-PAOLA-RECHAZO", "EXITOSO")).thenReturn(false);
        when(email2FAService.validarCodigo2FA(3, "5730094736", "123456")).thenReturn(true);
        when(saldoRepository.findByCuentaIdForUpdate(3)).thenReturn(Optional.of(saldoTest));

        Pago pagoMock = Pago.builder()
                .id(102L)
                .numeroCuenta("5730094736")
                .cliente(clienteTest)
                .monto(new BigDecimal("250.00"))
                .referencia("REF-PAOLA-RECHAZO")
                .estado("EN_PROCESO")
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(pagoRepository.save(any(Pago.class))).thenReturn(pagoMock);

        GestoPagoTxResponse txRechazo = GestoPagoTxResponse.builder()
                .exitoso(false)
                .timeout(false)
                .codigo(400)
                .mensaje("Producto sin inventario en el proveedor")
                .build();
        when(gestoPagoTxService.enviarTransaccion(any(), any(), any(), any(), any(), any())).thenReturn(txRechazo);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("rechazada por el proveedor Gestopago"));
        // El saldo debe haberse reintegrado a $1,000.00 disponible y $0.00 retenido
        assertEquals(0, new BigDecimal("1000.00").compareTo(saldoTest.getSaldoDisponible()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saldoTest.getSaldoRetenido()));
    }
}
