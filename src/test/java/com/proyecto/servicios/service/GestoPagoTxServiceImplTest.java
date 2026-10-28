package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.service.Impl.GestoPagoTxServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTxServiceImplTest {

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @InjectMocks
    private GestoPagoTxServiceImpl gestoPagoTxService;

    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayload";
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gestoPagoTxService, "apiKey", "TEST_API_KEY");
        ReflectionTestUtils.setField(gestoPagoTxService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gestoPagoTxService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(gestoPagoTxService, "password", "12345678");

        GestoPagoToken tokenEntity = new GestoPagoToken();
        tokenEntity.setToken(TEST_TOKEN);
        lenient().when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(tokenEntity));

        dummyRequest = Request.create(
                Request.HttpMethod.POST,
                "/sistema/service/sendTx.do",
                new HashMap<>(),
                null,
                new RequestTemplate()
        );
    }

    @Test
    @DisplayName("Debe procesar exitosamente la transacción sendTx.do con Bearer Token y parsear XML")
    void testEnviarTransaccionExitosa() {
        String xmlRespuesta = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<TRANSACCION>"
                + "<VALID>1</VALID>"
                + "<CODIGO>0</CODIGO>"
                + "<STATUS>EXITOSA</STATUS>"
                + "<MENSAJE>Recarga exitosa</MENSAJE>"
                + "<FOLIO>GP-998877</FOLIO>"
                + "<AUTORIZACION>AUTH-554433</AUTORIZACION>"
                + "<FECHA>2026-10-27</FECHA>"
                + "<HORA>22:45:00</HORA>"
                + "</TRANSACCION>";

        when(gestoPagoAuthClient.sendTx(
                eq("Bearer " + TEST_TOKEN),
                eq(83),
                eq("GPS83-TPV-17"),
                eq(1),
                eq(1),
                eq(new BigDecimal("250.00")),
                eq("REF-PAOLA-001"),
                any(),
                any()
        )).thenReturn(xmlRespuesta);

        GestoPagoTxResponse response = gestoPagoTxService.enviarTransaccion(
                1, 1, new BigDecimal("250.00"), "REF-PAOLA-001", "1234567890", "1"
        );

        assertNotNull(response);
        assertTrue(response.isExitoso());
        assertFalse(response.isTimeout());
        assertEquals(0, response.getCodigo());
        assertEquals("GP-998877", response.getFolio());
        assertEquals("AUTH-554433", response.getAutorizacion());
        assertEquals("Recarga exitosa", response.getMensaje());
    }

    @Test
    @DisplayName("Debe marcar timeout=true y NO asumir fallo ante GatewayTimeout (504) o RetryableException")
    void testEnviarTransaccionGatewayTimeout() {
        RetryableException timeoutException = new RetryableException(
                504, "Gateway Timeout", Request.HttpMethod.POST, 1000L, dummyRequest
        );

        when(gestoPagoAuthClient.sendTx(
                anyString(), anyInt(), anyString(), anyInt(), anyInt(), any(), anyString(), any(), any()
        )).thenThrow(timeoutException);

        GestoPagoTxResponse response = gestoPagoTxService.enviarTransaccion(
                1, 1, new BigDecimal("250.00"), "REF-PAOLA-002", "1234567890", "1"
        );

        assertNotNull(response);
        assertFalse(response.isExitoso());
        assertTrue(response.isTimeout(), "Debe marcar timeout=true para no asumir fallo y requerir conciliación");
        assertEquals(504, response.getCodigo());
        assertTrue(response.getMensaje().contains("conciliación"));
    }

    @Test
    @DisplayName("Debe detectar SocketTimeoutException y requerir conciliación")
    void testEnviarTransaccionSocketTimeout() {
        SocketTimeoutException socketTimeout = new SocketTimeoutException("Read timed out");
        FeignException feignTimeout = new FeignException.GatewayTimeout(
                "Socket Timeout", dummyRequest, null, null
        );
        ReflectionTestUtils.setField(feignTimeout, "cause", socketTimeout);

        when(gestoPagoAuthClient.sendTx(
                anyString(), anyInt(), anyString(), anyInt(), anyInt(), any(), anyString(), any(), any()
        )).thenThrow(feignTimeout);

        GestoPagoTxResponse response = gestoPagoTxService.enviarTransaccion(
                1, 1, new BigDecimal("250.00"), "REF-PAOLA-003", "1234567890", "1"
        );

        assertNotNull(response);
        assertTrue(response.isTimeout());
        assertEquals(504, response.getCodigo());
    }

    @Test
    @DisplayName("Debe reportar rechazo explícito cuando el proveedor retorna error")
    void testEnviarTransaccionRechazoProveedor() {
        String xmlRechazo = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<TRANSACCION>"
                + "<VALID>0</VALID>"
                + "<CODIGO>105</CODIGO>"
                + "<STATUS>RECHAZADA</STATUS>"
                + "<MENSAJE>Número telefónico inválido o sin saldo de distribuidor</MENSAJE>"
                + "</TRANSACCION>";

        when(gestoPagoAuthClient.sendTx(
                anyString(), anyInt(), anyString(), anyInt(), anyInt(), any(), anyString(), any(), any()
        )).thenReturn(xmlRechazo);

        GestoPagoTxResponse response = gestoPagoTxService.enviarTransaccion(
                1, 1, new BigDecimal("250.00"), "REF-PAOLA-004", "1234567890", "1"
        );

        assertNotNull(response);
        assertFalse(response.isExitoso());
        assertFalse(response.isTimeout());
        assertEquals(105, response.getCodigo());
        assertTrue(response.getMensaje().contains("inválido"));
    }
}
