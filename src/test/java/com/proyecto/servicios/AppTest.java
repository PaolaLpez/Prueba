package com.proyecto.servicios;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.cuenta.SaldoRepository;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppTest {

    @Mock
    private ApplicationContext context;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoService productoService;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository usuarioLoginRepository;

    @InjectMocks
    private App app;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(app, "context", context);
        ReflectionTestUtils.setField(app, "productoRepository", productoRepository);
        ReflectionTestUtils.setField(app, "productoService", productoService);
        lenient().when(context.getBean(CuentaRepository.class)).thenReturn(cuentaRepository);
        lenient().when(context.getBean(ClienteRepository.class)).thenReturn(clienteRepository);
        lenient().when(context.getBean(SaldoRepository.class)).thenReturn(saldoRepository);
        lenient().when(context.getBean(com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository.class)).thenReturn(usuarioLoginRepository);
    }

    @Test
    @DisplayName("Debe ejecutar run() actualizando cliente y cuenta existentes")
    void testRun_ClienteYCuentaExistentes() {
        when(productoRepository.existsById("1")).thenReturn(true);

        Cliente cli = new Cliente();
        cli.setId(3);
        cli.setCorreo("lopezpao869@gmail.com");
        cli.setTelefonoMovil("5512345678");
        cli.setActivo(true);
        when(clienteRepository.findByCorreo("lopezpao869@gmail.com")).thenReturn(Optional.of(cli));

        Cuenta cta = new Cuenta();
        cta.setId(3);
        cta.setNumeroCuenta("5730094736");
        cta.setCliente(cli);
        cta.setEstatus("ACTIVA");
        when(cuentaRepository.findByNumeroCuenta("5730094736")).thenReturn(Optional.of(cta));

        Saldo sld = new Saldo();
        sld.setId(3);
        sld.setSaldoDisponible(new BigDecimal("1500.00"));
        when(saldoRepository.findByCuentaId(3)).thenReturn(Optional.of(sld));

        assertDoesNotThrow(() -> app.run());
    }

    @Test
    @DisplayName("Debe ejecutar run() creando cliente, cuenta y saldo nuevos cuando no existen")
    void testRun_CrearNuevosRegistros() {
        when(productoRepository.existsById("1")).thenReturn(false);

        when(clienteRepository.findByCorreo("lopezpao869@gmail.com")).thenReturn(Optional.empty());
        when(clienteRepository.findById(3)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(3);
            return c;
        });

        when(cuentaRepository.findByNumeroCuenta("5730094736")).thenReturn(Optional.empty());
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> {
            Cuenta cta = inv.getArgument(0);
            cta.setId(3);
            return cta;
        });

        assertDoesNotThrow(() -> app.run());

        verify(clienteRepository, atLeastOnce()).save(any(Cliente.class));
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
        verify(saldoRepository, times(1)).save(any(Saldo.class));
    }

    @Test
    @DisplayName("Debe ejecutar run() cuando saldo está por debajo del umbral mínimo de 250")
    void testRun_SaldoBajo() {
        when(productoRepository.existsById("1")).thenReturn(true);

        Cliente cli = new Cliente();
        cli.setId(3);
        cli.setCorreo("antiguo@gmail.com"); // probar rama de actualización de correo y teléfono
        cli.setTelefonoMovil("0000000000");
        cli.setActivo(false);
        when(clienteRepository.findByCorreo("lopezpao869@gmail.com")).thenReturn(Optional.of(cli));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cli);

        Cuenta cta = new Cuenta();
        cta.setId(3);
        cta.setNumeroCuenta("5730094736");
        cta.setCliente(cli);
        cta.setEstatus("INACTIVA");
        when(cuentaRepository.findByNumeroCuenta("5730094736")).thenReturn(Optional.of(cta));

        Saldo sld = new Saldo();
        sld.setId(3);
        sld.setSaldoDisponible(new BigDecimal("50.00")); // saldo bajo
        when(saldoRepository.findByCuentaId(3)).thenReturn(Optional.of(sld));

        assertDoesNotThrow(() -> app.run());

        verify(saldoRepository, times(1)).save(sld);
    }

    @Test
    @DisplayName("Debe tolerar excepciones en run() sin propagar error crítico")
    void testRun_ConExcepciones() {
        doThrow(new RuntimeException("Simulated error")).when(productoService).obtenerProductos();
        when(productoRepository.existsById("1")).thenThrow(new RuntimeException("DB error"));
        when(context.getBean(CuentaRepository.class)).thenThrow(new RuntimeException("Context error"));

        assertDoesNotThrow(() -> app.run());
    }
}
