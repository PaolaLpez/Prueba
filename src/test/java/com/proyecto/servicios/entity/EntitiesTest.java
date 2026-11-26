package com.proyecto.servicios.entity;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.Saldo;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.pago.Pago;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.entity.seguridad.DatosBiometricos;
import com.proyecto.servicios.entity.seguridad.UsuarioLogin;
import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.model.dto.error.ApiErrorResponse;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.dto.pago.PagoConfirmacionResponse;
import com.proyecto.servicios.model.dto.pago.PagoRequest;
import com.proyecto.servicios.model.dto.pago.Respuesta2FA;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class EntitiesTest {

    @Test
    @DisplayName("Debe verificar getters, setters y builders de todas las entidades JPA")
    void testEntitiesGettersAndSetters() {
        // 1. Cliente y Domicilio
        Cliente cli = new Cliente();
        cli.setId(1);
        cli.setNombre("Paola");
        cli.setSegundoNombre("Sofia");
        cli.setApellidoPaterno("Lopez");
        cli.setApellidoMaterno("Garcia");
        cli.setFechaNacimiento(LocalDate.of(1998, 5, 15));
        cli.setCurp("LOGP980515MMCPRL09");
        cli.setRfc("LOGP980515AA0");
        cli.setSexo("FEMENINO");
        cli.setNacionalidad("MEXICANA");
        cli.setEstadoCivil("SOLTERO");
        cli.setCorreo("paola@gmail.com");
        cli.setTelefonoMovil("5512345678");
        cli.setTelefonoAlternativo("5587654321");
        cli.setOcupacion("Ingeniera");
        cli.setEmpresa("Tech");
        cli.setIngresoMensual(new BigDecimal("30000.00"));
        cli.setActivo(true);
        cli.setFechaCreacion(LocalDateTime.now());
        cli.setDomicilios(new ArrayList<>());

        Domicilio dom = new Domicilio();
        dom.setId(1);
        dom.setCalle("Insurgentes");
        dom.setNumeroExterior("100");
        dom.setNumeroInterior("1");
        dom.setColonia("Roma");
        dom.setMunicipio("Cuauhtemoc");
        dom.setEstado("CDMX");
        dom.setCodigoPostal("06700");
        dom.setPais("Mexico");
        cli.addDomicilio(dom);

        assertEquals(1, cli.getId());
        assertEquals("Paola", cli.getNombre());
        assertEquals("Sofia", cli.getSegundoNombre());
        assertEquals("Lopez", cli.getApellidoPaterno());
        assertEquals("Garcia", cli.getApellidoMaterno());
        assertEquals("LOGP980515MMCPRL09", cli.getCurp());
        assertEquals("LOGP980515AA0", cli.getRfc());
        assertEquals("FEMENINO", cli.getSexo());
        assertEquals("MEXICANA", cli.getNacionalidad());
        assertEquals("SOLTERO", cli.getEstadoCivil());
        assertEquals("paola@gmail.com", cli.getCorreo());
        assertEquals("5512345678", cli.getTelefonoMovil());
        assertEquals("5587654321", cli.getTelefonoAlternativo());
        assertEquals("Ingeniera", cli.getOcupacion());
        assertEquals("Tech", cli.getEmpresa());
        assertEquals(new BigDecimal("30000.00"), cli.getIngresoMensual());
        assertTrue(cli.getActivo());
        assertNotNull(cli.getFechaCreacion());
        assertEquals(1, cli.getDomicilios().size());
        assertEquals(cli, dom.getCliente());
        assertEquals("Insurgentes", dom.getCalle());
        assertEquals("100", dom.getNumeroExterior());
        assertEquals("1", dom.getNumeroInterior());
        assertEquals("Roma", dom.getColonia());
        assertEquals("Cuauhtemoc", dom.getMunicipio());
        assertEquals("CDMX", dom.getEstado());
        assertEquals("06700", dom.getCodigoPostal());
        assertEquals("Mexico", dom.getPais());

        // 2. Cuenta y Saldo
        Cuenta cta = new Cuenta();
        cta.setId(1);
        cta.setNumeroCuenta("5730094736");
        cta.setEstatus("ACTIVA");
        cta.setCliente(cli);
        cta.setFechaApertura(LocalDateTime.now());

        Saldo sld = new Saldo();
        sld.setId(1);
        sld.setCuenta(cta);
        sld.setSaldoDisponible(new BigDecimal("1500.00"));
        sld.setSaldoRetenido(BigDecimal.ZERO);
        sld.setFechaActualizacion(LocalDateTime.now());
        cta.setSaldo(sld);

        assertEquals("5730094736", cta.getNumeroCuenta());
        assertEquals("ACTIVA", cta.getEstatus());
        assertEquals(cli, cta.getCliente());
        assertNotNull(cta.getFechaApertura());
        assertEquals(new BigDecimal("1500.00"), sld.getSaldoDisponible());
        assertEquals(BigDecimal.ZERO, sld.getSaldoRetenido());
        assertEquals(cta, sld.getCuenta());
        assertNotNull(sld.getFechaActualizacion());

        // 3. UsuarioLogin y DatosBiometricos
        UsuarioLogin login = new UsuarioLogin();
        login.setId(1);
        login.setUsername("paola");
        login.setPasswordHash("hash123");
        login.setActivo(true);
        login.setCliente(cli);
        login.setUltimoAcceso(LocalDateTime.now());

        assertEquals("paola", login.getUsername());
        assertEquals("hash123", login.getPasswordHash());
        assertTrue(login.getActivo());
        assertEquals(cli, login.getCliente());
        assertNotNull(login.getUltimoAcceso());

        DatosBiometricos bio = new DatosBiometricos();
        bio.setId(1);
        bio.setCliente(cli);
        bio.setTipoBiometrico("MEDIAPIPE");
        bio.setFaceEmbeddingCifrado("CIFRADO123");
        bio.setFechaRegistro(LocalDateTime.now());

        assertEquals(cli, bio.getCliente());
        assertEquals("MEDIAPIPE", bio.getTipoBiometrico());
        assertEquals("CIFRADO123", bio.getFaceEmbeddingCifrado());
        assertNotNull(bio.getFechaRegistro());

        // 4. Pago
        Pago pago = Pago.builder()
                .id(1L)
                .numeroCuenta("5730094736")
                .cliente(cli)
                .productoId("1")
                .monto(new BigDecimal("250.00"))
                .telefono("5512345678")
                .referencia("REF-1")
                .horaLocal("12:00:00")
                .upc("1234567890")
                .unidad("1")
                .peticionCifrada("CIFRADO")
                .token("TOKEN")
                .estado("EXITOSO")
                .trace("TRACE-1")
                .fechaCreacion(LocalDateTime.now())
                .build();

        assertEquals(1L, pago.getId());
        assertEquals("REF-1", pago.getReferencia());
        assertEquals("EXITOSO", pago.getEstado());
        assertEquals("1", pago.getProductoId());
        assertEquals("TRACE-1", pago.getTrace());
        assertEquals("5730094736", pago.getNumeroCuenta());
        assertEquals(new BigDecimal("250.00"), pago.getMonto());
        assertEquals("5512345678", pago.getTelefono());
        assertEquals("12:00:00", pago.getHoraLocal());
        assertEquals("1234567890", pago.getUpc());
        assertEquals("1", pago.getUnidad());
        assertEquals("CIFRADO", pago.getPeticionCifrada());
        assertEquals("TOKEN", pago.getToken());
        assertNotNull(pago.getFechaCreacion());
        assertEquals(cli, pago.getCliente());

        // 5. GestoPagoToken
        GestoPagoToken token = new GestoPagoToken();
        token.setId(1);
        token.setIdDistribuidor(83);
        token.setCodigoDispositivo("GPS83");
        token.setToken("TOKEN-1");
        token.setActivo(true);
        token.setFechaCreacion(LocalDateTime.now());
        token.setFechaActualizacion(LocalDateTime.now());

        assertEquals(83, token.getIdDistribuidor());
        assertEquals("GPS83", token.getCodigoDispositivo());
        assertEquals("TOKEN-1", token.getToken());
        assertTrue(token.getActivo());

        // 6. Personas
        Personas p = new Personas();
        p.setId(1);
        p.setNombre("Carlos");
        p.setApellidoP("Hernandez");
        p.setApellidoMaterno("Diaz");

        assertEquals(1, p.getId());
        assertEquals("Carlos", p.getNombre());
        assertEquals("Hernandez", p.getApellidoP());
        assertEquals("Diaz", p.getApellidoMaterno());

        // 7. Producto
        Producto prod = new Producto();
        prod.setId("1");
        prod.setIdProducto(1);
        prod.setIdServicio(1);
        prod.setIdCatTipoServicio(1);
        prod.setTipoFront(1);
        prod.setNombre("Recarga");
        prod.setPrecio(new BigDecimal("100.00"));
        prod.setCategoria("Servicios");
        prod.setDisponible(true);
        prod.setHasDigitoVerificador(false);
        prod.setShowAyuda(false);
        prod.setTipoReferencia("NUM");

        assertEquals("1", prod.getId());
        assertEquals(1, prod.getIdProducto());
        assertEquals(1, prod.getIdServicio());
        assertEquals(1, prod.getIdCatTipoServicio());
        assertEquals(1, prod.getTipoFront());
        assertEquals("Recarga", prod.getNombre());
        assertEquals(new BigDecimal("100.00"), prod.getPrecio());
        assertEquals("Servicios", prod.getCategoria());
        assertTrue(prod.getDisponible());
        assertFalse(prod.getHasDigitoVerificador());
        assertFalse(prod.getShowAyuda());
        assertEquals("NUM", prod.getTipoReferencia());
    }

    @Test
    @DisplayName("Debe verificar DTOs de peticiones y respuestas")
    void testDTOs() {
        PagoRequest req = PagoRequest.builder()
                .numeroCuenta("123")
                .idCliente(1)
                .idProducto("1")
                .nombreProducto("Prod")
                .idServicio(1)
                .monto(new BigDecimal("100.00"))
                .telefono("5512345678")
                .referencia("REF-1")
                .dobleAutenticacion("123456")
                .correo("test@test.com")
                .unidad("1")
                .build();
        assertNotNull(req.getCorreo());

        PagoConfirmacionResponse resp = PagoConfirmacionResponse.builder()
                .state(true)
                .code("00")
                .codigo(0)
                .message("OK")
                .mensaje("OK")
                .build();
        assertTrue(resp.isState());

        Respuesta2FA r2fa = Respuesta2FA.builder()
                .state(true)
                .code("00")
                .codigo(0)
                .message("Enviado")
                .mensaje("Enviado")
                .correoDestino("t***@test.com")
                .tiempoValidezSegundos(300)
                .fechaExpiracion(LocalDateTime.now())
                .build();
        assertTrue(r2fa.getState());

        ApiErrorResponse err = ApiErrorResponse.builder()
                .status(400)
                .error("Error")
                .mensaje("Detalle")
                .timestamp(LocalDateTime.now())
                .build();
        assertEquals(400, err.getStatus());

        GestoPagoTxResponse txResp = GestoPagoTxResponse.builder()
                .exitoso(true)
                .codigo(0)
                .mensaje("OK")
                .folio("FOL")
                .autorizacion("AUTH")
                .build();
        assertTrue(txResp.isExitoso());

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setToken("TOKEN");
        authResp.setStatus(200);
        authResp.setMessage("OK");
        assertEquals("TOKEN", authResp.getToken());
        assertEquals(200, authResp.getStatus());

        PersonasRequest preq = new PersonasRequest();
        preq.setNombre("A");
        preq.setApellidoP("B");
        preq.setApellidoMaterno("C");
        assertEquals("A", preq.getNombre());

        PersonaResponse presp = new PersonaResponse();
        presp.setCodigo(1);
        presp.setMensaje("OK");
        assertEquals(1, presp.getCodigo());

        EliminaPersonaRequest ereq = new EliminaPersonaRequest();
        ereq.setNombre("X");
        assertEquals("X", ereq.getNombre());

        GenericResponse gresp = new GenericResponse();
        gresp.setCodigo(0);
        gresp.setMensaje("OK");
        assertEquals(0, gresp.getCodigo());
    }
}
