package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.exception.ClienteDuplicadoException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteBusquedaRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.model.dto.cliente.DomicilioDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioLoginRepository usuarioLoginRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Cliente clienteTest;
    private Cuenta cuentaTest;

    @BeforeEach
    void setUp() {
        clienteTest = new Cliente();
        clienteTest.setId(1);
        clienteTest.setNombre("Paola");
        clienteTest.setApellidoPaterno("Lopez");
        clienteTest.setApellidoMaterno("Garcia");
        clienteTest.setCurp("LOGP980515MMCPRL09");
        clienteTest.setRfc("LOGP980515AA0");
        clienteTest.setCorreo("paola@gmail.com");
        clienteTest.setTelefonoMovil("5512345678");
        clienteTest.setFechaNacimiento(LocalDate.of(1998, 5, 15));
        clienteTest.setActivo(true);

        cuentaTest = new Cuenta();
        cuentaTest.setId(1);
        cuentaTest.setNumeroCuenta("5730094736");
        cuentaTest.setEstatus("ACTIVA");
        cuentaTest.setCliente(clienteTest);

        Saldo saldo = new Saldo();
        saldo.setSaldoDisponible(new BigDecimal("1500.00"));
        cuentaTest.setSaldo(saldo);
    }

    @Test
    @DisplayName("Debe consultar todos los clientes registrados")
    void testConsultarTodosClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        List<ClienteResponse> lista = clienteService.consultarTodosClientes();

        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertEquals("Paola", lista.get(0).getNombre());
    }

    @Test
    @DisplayName("Debe consultar cliente por ID")
    void testConsultarPorId_Exitoso() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        ClienteResponse response = clienteService.consultarPorId(1);

        assertNotNull(response);
        assertEquals(1, response.getId());
    }

    @Test
    @DisplayName("Debe fallar al consultar por ID inexistente")
    void testConsultarPorId_NoExiste() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.consultarPorId(99));
    }

    @Test
    @DisplayName("Debe consultar cliente por CURP")
    void testConsultarPorCurp() {
        when(clienteRepository.findByCurp("LOGP980515MMCPRL09")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        ClienteResponse response = clienteService.consultarPorCurp("logp980515mmcprl09");

        assertNotNull(response);
        assertEquals("LOGP980515MMCPRL09", response.getCurp());
    }

    @Test
    @DisplayName("Debe fallar al consultar por CURP inexistente")
    void testConsultarPorCurp_NoExiste() {
        when(clienteRepository.findByCurp(anyString())).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.consultarPorCurp("LOGP980515MMCPRL09"));
    }

    @Test
    @DisplayName("Debe consultar cliente por RFC")
    void testConsultarPorRfc() {
        when(clienteRepository.findByRfc("LOGP980515AA0")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        ClienteResponse response = clienteService.consultarPorRfc("logp980515aa0");

        assertNotNull(response);
        assertEquals("LOGP980515AA0", response.getRfc());
    }

    @Test
    @DisplayName("Debe fallar al consultar por RFC inexistente")
    void testConsultarPorRfc_NoExiste() {
        when(clienteRepository.findByRfc(anyString())).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.consultarPorRfc("LOGP980515AA0"));
    }

    @Test
    @DisplayName("Debe consultar cliente por correo")
    void testConsultarPorCorreo() {
        when(clienteRepository.findByCorreo("paola@gmail.com")).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        ClienteResponse response = clienteService.consultarPorCorreo("paola@gmail.com");

        assertNotNull(response);
        assertEquals("paola@gmail.com", response.getCorreo());
    }

    @Test
    @DisplayName("Debe fallar al consultar por correo inexistente")
    void testConsultarPorCorreo_NoExiste() {
        when(clienteRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.consultarPorCorreo("noexiste@gmail.com"));
    }

    @Test
    @DisplayName("Debe consultar cliente por número de cuenta bancaria")
    void testConsultarPorNumeroCuenta() {
        when(cuentaRepository.findByNumeroCuenta("5730094736")).thenReturn(Optional.of(cuentaTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        ClienteResponse response = clienteService.consultarPorNumeroCuenta("5730094736");

        assertNotNull(response);
        assertEquals(1, response.getId());
    }

    @Test
    @DisplayName("Debe fallar al consultar por número de cuenta inexistente")
    void testConsultarPorNumeroCuenta_NoExiste() {
        when(cuentaRepository.findByNumeroCuenta(anyString())).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.consultarPorNumeroCuenta("9999999999"));
    }

    @Test
    @DisplayName("Debe consultar clientes activos")
    void testConsultarClientesActivos() {
        when(clienteRepository.findByActivoTrue()).thenReturn(List.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        List<ClienteResponse> activos = clienteService.consultarClientesActivos();
        assertNotNull(activos);
        assertEquals(1, activos.size());
    }

    @Test
    @DisplayName("Debe consultar clientes por rango de fechas")
    void testConsultarClientesPorRangoFechas_Exitoso() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 1, 15);
        when(clienteRepository.findByFechaCreacionBetween(any(), any())).thenReturn(List.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        List<ClienteResponse> res = clienteService.consultarClientesPorRangoFechas(inicio, fin);
        assertNotNull(res);
        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("Debe validar fechas de consulta por rango")
    void testConsultarClientesPorRangoFechas_Validaciones() {
        assertThrows(ReglaNegocioException.class, () -> clienteService.consultarClientesPorRangoFechas(null, LocalDate.now()));
        assertThrows(ReglaNegocioException.class, () -> clienteService.consultarClientesPorRangoFechas(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)));
        assertThrows(ReglaNegocioException.class, () -> clienteService.consultarClientesPorRangoFechas(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1)));
        assertThrows(ReglaNegocioException.class, () -> clienteService.consultarClientesPorRangoFechas(LocalDate.now().plusDays(2), LocalDate.now().plusDays(5)));
    }

    @Test
    @DisplayName("Debe registrar cliente exitosamente con cuenta y credenciales")
    void testRegistrarCliente_Exitoso() {
        DomicilioDto domicilio = DomicilioDto.builder()
                .calle("Av Insurgentes Sur")
                .numeroExterior("100")
                .colonia("Roma")
                .municipio("Cuauhtemoc")
                .estado("CDMX")
                .codigoPostal("06700")
                .build();

        ClienteRegistroRequest request = new ClienteRegistroRequest();
        request.setNombre("Paola");
        request.setApellidoPaterno("Lopez");
        request.setApellidoMaterno("Garcia");
        request.setFechaNacimiento(LocalDate.of(1998, 5, 15));
        request.setCurp("LOGP980515MMCPRL09");
        request.setRfc("LOGP980515AA0");
        request.setSexo("FEMENINO");
        request.setNacionalidad("MEXICANA");
        request.setEstadoCivil("SOLTERO");
        request.setCorreo("paola.nueva@gmail.com");
        request.setTelefonoMovil("5512345678");
        request.setPassword("PasswordSegura#123");
        request.setSaldoInicial(new BigDecimal("1000.00"));
        request.setIngresoMensual(new BigDecimal("25000.00"));
        request.setDomicilio(domicilio);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(10);
            return c;
        });
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse response = clienteService.registrarCliente(request);

        assertNotNull(response);
        assertEquals("Paola", response.getNombre());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
        verify(usuarioLoginRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Debe validar formato de nombres y campos de texto")
    void testValidacionesNombres() {
        ClienteRegistroRequest req = new ClienteRegistroRequest();
        req.setNombre("Al"); // < 3
        req.setApellidoPaterno("Lopez");
        req.setApellidoMaterno("Garcia");
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setNombre("Paola123"); // números
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setNombre("Paola");
        req.setApellidoPaterno(""); // vacío
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("Debe validar formato de CURP, RFC, correo, teléfono, sexo y nacionalidad")
    void testValidacionesFormatos() {
        ClienteRegistroRequest req = new ClienteRegistroRequest();
        req.setNombre("Paola");
        req.setApellidoPaterno("Lopez");
        req.setApellidoMaterno("Garcia");

        req.setCurp("INVALID_CURP");
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setCurp("LOGP980515MMCPRL09");
        req.setRfc("INVALID_RFC");
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setRfc("LOGP980515AA0");
        req.setCorreo("correo_invalido");
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setCorreo("paola@gmail.com");
        req.setTelefonoMovil("123"); // not 10 digits
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setTelefonoMovil("5512345678");
        req.setSexo("INVALID_SEX");
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setSexo("FEMENINO");
        req.setNacionalidad("MARCIANA");
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setNacionalidad("MEXICANA");
        req.setEstadoCivil("ESTADO_INEXISTENTE");
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("Debe validar seguridad de la contraseña")
    void testValidarPasswordSegura() {
        ClienteRegistroRequest req = new ClienteRegistroRequest();
        req.setNombre("Paola");
        req.setApellidoPaterno("Lopez");
        req.setApellidoMaterno("Garcia");
        req.setCurp("LOGP980515MMCPRL09");
        req.setRfc("LOGP980515AA0");
        req.setCorreo("paola@gmail.com");
        req.setTelefonoMovil("5512345678");
        req.setSexo("FEMENINO");
        req.setNacionalidad("MEXICANA");
        req.setEstadoCivil("SOLTERO");

        req.setPassword("corta"); // < 8
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setPassword("solominusculas123#"); // no uppercase
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setPassword("SOLOMAYUSCULAS123#"); // no lowercase
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setPassword("SoloLetrasYSigno#"); // no digit
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setPassword("PasswordSinEspecial123"); // no special char
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("Debe validar unicidad y detectar duplicados")
    void testValidarUnicidadDuplicados() {
        ClienteRegistroRequest req = new ClienteRegistroRequest();
        req.setNombre("Paola");
        req.setApellidoPaterno("Lopez");
        req.setApellidoMaterno("Garcia");
        req.setCurp("LOGP980515MMCPRL09");
        req.setRfc("LOGP980515AA0");
        req.setCorreo("paola@gmail.com");
        req.setTelefonoMovil("5512345678");
        req.setSexo("FEMENINO");
        req.setNacionalidad("MEXICANA");
        req.setEstadoCivil("SOLTERO");
        req.setPassword("PasswordSegura#123");
        req.setFechaNacimiento(LocalDate.of(1998, 5, 15));

        when(clienteRepository.existsByCurpIgnoreCase("LOGP980515MMCPRL09")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.registrarCliente(req));

        when(clienteRepository.existsByCurpIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfcIgnoreCase("LOGP980515AA0")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.registrarCliente(req));

        when(clienteRepository.existsByRfcIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoIgnoreCase("paola@gmail.com")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.registrarCliente(req));

        when(clienteRepository.existsByCorreoIgnoreCase(anyString())).thenReturn(false);
        when(usuarioLoginRepository.existsByUsername("paola@gmail.com")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.registrarCliente(req));

        when(usuarioLoginRepository.existsByUsername(anyString())).thenReturn(false);
        when(clienteRepository.existsByTelefonoMovil("5512345678")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.registrarCliente(req));

        when(clienteRepository.existsByTelefonoMovil(anyString())).thenReturn(false);
        when(clienteRepository.existsByNombreIgnoreCaseAndApellidoPaternoIgnoreCaseAndApellidoMaternoIgnoreCaseAndFechaNacimiento(any(), any(), any(), any())).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.registrarCliente(req));

        when(clienteRepository.existsByNombreIgnoreCaseAndApellidoPaternoIgnoreCaseAndApellidoMaternoIgnoreCaseAndFechaNacimiento(any(), any(), any(), any())).thenReturn(false);
        when(clienteRepository.existsByNombreIgnoreCaseAndApellidoPaternoIgnoreCaseAndApellidoMaternoIgnoreCase(any(), any(), any())).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("Debe fallar al registrar si es menor de edad")
    void testRegistrarCliente_MenorDeEdad() {
        ClienteRegistroRequest request = new ClienteRegistroRequest();
        request.setNombre("Paola");
        request.setApellidoPaterno("Lopez");
        request.setApellidoMaterno("Garcia");
        request.setFechaNacimiento(LocalDate.now().minusYears(15));
        request.setCurp("LOGP090515MMCPRL09");
        request.setRfc("LOGP090515AA0");
        request.setSexo("FEMENINO");
        request.setNacionalidad("MEXICANA");
        request.setEstadoCivil("SOLTERO");
        request.setCorreo("menor@gmail.com");
        request.setTelefonoMovil("5512345678");
        request.setPassword("PasswordSegura#123");

        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    @DisplayName("Debe actualizar cliente exitosamente")
    void testActualizarCliente_Exitoso() {
        ClienteActualizacionRequest request = new ClienteActualizacionRequest();
        request.setId(1);
        request.setNombre("Paola Sofia");
        request.setApellidoPaterno("Lopez");
        request.setApellidoMaterno("Garcia");
        request.setCurp("LOGP980515MMCPRL09");
        request.setRfc("LOGP980515AA0");
        request.setSexo("FEMENINO");
        request.setNacionalidad("MEXICANA");
        request.setEstadoCivil("CASADO");

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteTest);

        ClienteResponse response = clienteService.actualizarCliente(request);

        assertNotNull(response);
        verify(clienteRepository, times(1)).save(clienteTest);
    }

    @Test
    @DisplayName("Debe validar actualización con campos inválidos y duplicados")
    void testActualizarCliente_Validaciones() {
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(null));
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(new ClienteActualizacionRequest()));

        ClienteActualizacionRequest req = new ClienteActualizacionRequest();
        req.setId(1);
        req.setCurp("");
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(req));

        req.setCurp("LOGP980515MMCPRL09");
        req.setRfc("");
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(req));

        req.setRfc("LOGP980515AA0");
        when(clienteRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.actualizarCliente(req));

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        req.setCurp("OTRA980515MMCPRL01");
        when(clienteRepository.existsByCurp("OTRA980515MMCPRL01")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.actualizarCliente(req));

        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        req.setRfc("OTRA980515AA1");
        when(clienteRepository.existsByRfc("OTRA980515AA1")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.actualizarCliente(req));

        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        req.setCorreo("otro@gmail.com");
        when(clienteRepository.existsByCorreo("otro@gmail.com")).thenReturn(true);
        assertThrows(ClienteDuplicadoException.class, () -> clienteService.actualizarCliente(req));

        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        req.setSegundoNombre("A"); // < 3
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(req));

        req.setSegundoNombre("Sofia");
        req.setTelefonoAlternativo("123"); // not 10
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(req));

        req.setTelefonoAlternativo("5598765432");
        req.setIngresoMensual(BigDecimal.ZERO); // <= 0
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(req));

        req.setIngresoMensual(new BigDecimal("20000.00"));
        req.setDomicilio(DomicilioDto.builder().calle("Nueva").numeroExterior("1").municipio("Cuauhtemoc").build());
        when(clienteRepository.save(any())).thenReturn(clienteTest);

        ClienteResponse res = clienteService.actualizarCliente(req);
        assertNotNull(res);
    }

    @Test
    @DisplayName("Debe ejecutar baja lógica de cliente y sus cuentas")
    void testBajaLogicaCliente() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        when(cuentaRepository.findByClienteId(1)).thenReturn(List.of(cuentaTest));

        clienteService.bajaLogicaCliente(1);

        assertFalse(clienteTest.getActivo());
        assertEquals("INACTIVA", cuentaTest.getEstatus());
        verify(clienteRepository, times(1)).save(clienteTest);
        verify(cuentaRepository, times(1)).save(cuentaTest);
    }

    @Test
    @DisplayName("Debe buscar clientes dinámicamente usando Specification")
    void testBuscarClientesDinamico() {
        ClienteBusquedaRequest request = new ClienteBusquedaRequest();
        request.setNombre("Paola");
        request.setActivo(true);

        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of(clienteTest));

        List<ClienteResponse> resultado = clienteService.buscarClientesDinamico(request);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Debe buscar clientes dinámicamente con múltiples predicados")
    void testBuscarClientesDinamico_TodosPredicados() {
        ClienteBusquedaRequest request = new ClienteBusquedaRequest();
        request.setId(1);
        request.setCurp("LOGP980515MMCPRL09");
        request.setRfc("LOGP980515AA0");
        request.setCorreo("paola@gmail.com");
        request.setNombre("Paola");
        request.setApellidoPaterno("Lopez");
        request.setApellidoMaterno("Garcia");
        request.setTelefonoMovil("5512345678");
        request.setActivo(true);
        request.setFechaInicio(LocalDate.of(2026, 1, 1));
        request.setFechaFin(LocalDate.of(2026, 1, 10));

        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of(clienteTest));

        List<ClienteResponse> resultado = clienteService.buscarClientesDinamico(request);
        assertNotNull(resultado);

        // Prueba solo con fechaInicio
        request.setFechaFin(null);
        assertNotNull(clienteService.buscarClientesDinamico(request));

        // Prueba solo con fechaFin
        request.setFechaInicio(null);
        request.setFechaFin(LocalDate.of(2026, 1, 10));
        assertNotNull(clienteService.buscarClientesDinamico(request));
    }

    @Test
    @DisplayName("Debe registrar cliente sin saldo inicial, sin password y con campos secundarios")
    void testRegistrarCliente_CamposSecundarios() {
        ClienteRegistroRequest request = new ClienteRegistroRequest();
        request.setNombre("Paola");
        request.setSegundoNombre("Sofia");
        request.setApellidoPaterno("Lopez");
        request.setApellidoMaterno("Garcia");
        request.setFechaNacimiento(LocalDate.of(1998, 5, 15));
        request.setCurp("LOGP980515MMCPRL09");
        request.setRfc("LOGP980515AA0");
        request.setSexo("FEMENINO");
        request.setNacionalidad("MEXICANA");
        request.setEstadoCivil("SOLTERO");
        request.setCorreo("paola.secundaria@gmail.com");
        request.setTelefonoMovil("5512345678");
        request.setTelefonoAlternativo("5587654321");
        request.setOcupacion("Ingeniera");
        request.setEmpresa("Tech Corp");
        request.setSaldoInicial(null); // Defaults to 500.00
        request.setPassword(null); // No password
        request.setDomicilio(null); // No domicilio

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse response = clienteService.registrarCliente(request);

        assertNotNull(response);
        assertEquals("Sofia", response.getSegundoNombre());
        verify(usuarioLoginRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar todos los campos incluyendo domicilio existente")
    void testActualizarCliente_TodosLosCamposYDomicilioExistente() {
        com.proyecto.servicios.entity.cliente.Domicilio domExistente = new com.proyecto.servicios.entity.cliente.Domicilio();
        domExistente.setCalle("Calle Vieja");
        clienteTest.getDomicilios().add(domExistente);

        ClienteActualizacionRequest req = new ClienteActualizacionRequest();
        req.setId(1);
        req.setCurp("LOGP980515MMCPRL09");
        req.setRfc("LOGP980515AA0");
        req.setNombre("Paola");
        req.setSegundoNombre("Sofia");
        req.setApellidoPaterno("Lopez");
        req.setApellidoMaterno("Garcia");
        req.setFechaNacimiento(LocalDate.of(1998, 5, 15));
        req.setSexo("FEMENINO");
        req.setNacionalidad("MEXICANA");
        req.setEstadoCivil("CASADO");
        req.setCorreo("paola@gmail.com");
        req.setTelefonoMovil("5512345678");
        req.setTelefonoAlternativo("5511223344");
        req.setOcupacion("Arquitecta");
        req.setEmpresa("Design Co");
        req.setIngresoMensual(new BigDecimal("40000.00"));

        DomicilioDto domDto = DomicilioDto.builder()
                .calle("Av Reforma")
                .numeroExterior("222")
                .numeroInterior("5B")
                .colonia("Juarez")
                .municipio("Cuauhtemoc")
                .estado("CDMX")
                .codigoPostal("06600")
                .pais("Mexico")
                .build();
        req.setDomicilio(domDto);

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteTest);

        ClienteResponse resp = clienteService.actualizarCliente(req);
        assertNotNull(resp);
        assertEquals("Av Reforma", domExistente.getCalle());
        assertEquals("222", domExistente.getNumeroExterior());
    }

    @Test
    @DisplayName("Debe capturar excepciones inesperadas en actualizarCliente y lanzar ReglaNegocioException")
    void testActualizarCliente_ExcepcionInesperada() {
        ClienteActualizacionRequest req = new ClienteActualizacionRequest();
        req.setId(1);
        req.setCurp("LOGP980515MMCPRL09");
        req.setRfc("LOGP980515AA0");

        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));
        when(clienteRepository.save(any())).thenThrow(new RuntimeException("DB Connection error"));

        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCliente(req));
    }
}
