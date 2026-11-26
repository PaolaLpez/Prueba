package com.proyecto.servicios.controller;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoEchoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoControllerTest {

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @InjectMocks
    private GestoPagoController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "apiKey", "test-api");
        ReflectionTestUtils.setField(controller, "idDistribuidor", 83);
        ReflectionTestUtils.setField(controller, "codigoDispositivo", "GPS83");
        ReflectionTestUtils.setField(controller, "password", "12345");
    }

    @Test
    @DisplayName("Debe autenticar directamente con GestoPago")
    void testAutenticar() {
        GestoPagoAuthResponse mockResp = new GestoPagoAuthResponse();
        mockResp.setToken("TOKEN-1");
        when(gestoPagoAuthClient.authenticate(anyString(), anyInt(), anyString(), anyString())).thenReturn(mockResp);

        ResponseEntity<GestoPagoAuthResponse> response = controller.autenticar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("TOKEN-1", response.getBody().getToken());
    }

    @Test
    @DisplayName("Debe ejecutar echo procesando respuesta XML con VALID=1")
    void testSendEcho_Valido() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("TOKEN-ECHO");
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(token));

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response><ECHO>TEST_ECHO</ECHO><VALID>1</VALID></response>";
        when(gestoPagoAuthClient.sendEcho(anyString())).thenReturn(xml);

        ResponseEntity<GestoPagoEchoResponse> response = controller.sendEcho();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("TEST_ECHO", response.getBody().getEcho());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals("Conectividad validada", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe ejecutar echo procesando respuesta XML con VALID=0")
    void testSendEcho_Invalido() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("Bearer TOKEN-YA-CON-BEARER");
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(token));

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response><ECHO>ECHO_FAIL</ECHO><VALID>0</VALID></response>";
        when(gestoPagoAuthClient.sendEcho(anyString())).thenReturn(xml);

        ResponseEntity<GestoPagoEchoResponse> response = controller.sendEcho();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getCodigo());
        assertEquals("Conectividad no validada", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe fallar sendEcho si no hay token disponible")
    void testSendEcho_SinToken() {
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.empty());
        when(gestoPagoAuthClient.authenticate(anyString(), anyInt(), anyString(), anyString())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> controller.sendEcho());
    }

    @Test
    @DisplayName("Debe fallar sendEcho si la respuesta XML es corrupta")
    void testSendEcho_XmlInvalido() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("TOKEN");
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(token));
        when(gestoPagoAuthClient.sendEcho(anyString())).thenReturn("NO_ES_XML");

        assertThrows(IllegalStateException.class, () -> controller.sendEcho());
    }

    @Test
    @DisplayName("Debe ejecutar renovar-token")
    void testRenovarToken() {
        ResponseEntity<String> response = controller.renovarToken();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(gestoPagoTokenService, times(1)).renovarToken();
    }
}
