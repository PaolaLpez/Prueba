package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.BrevoEmailClient;
import com.proyecto.servicios.model.dto.brevo.BrevoEmailResponse;
import com.proyecto.servicios.service.Impl.BrevoEmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevoEmailServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BrevoEmailClient brevoEmailClient;

    @InjectMocks
    private BrevoEmailServiceImpl brevoEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(brevoEmailService, "senderEmail", "isapaola188@gmail.com");
        ReflectionTestUtils.setField(brevoEmailService, "senderName", "Sistema de Pagos Seguros");
        ReflectionTestUtils.setField(brevoEmailService, "brevoEmailClient", brevoEmailClient);
    }

    @Test
    @DisplayName("Debe construir HTML de código 2FA correctamente")
    void testConstruirHtml2FA() {
        String html = brevoEmailService.construirHtml2FA("Paola Lopez", "123456", "$250.00", "REF-TEST-001");

        assertNotNull(html);
        assertTrue(html.contains("Paola Lopez"));
        assertTrue(html.contains("123456"));
        assertTrue(html.contains("$250.00"));
        assertTrue(html.contains("REF-TEST-001"));
    }

    @Test
    @DisplayName("Debe construir HTML de comprobante de pago exitoso")
    void testConstruirHtmlComprobante() {
        String html = brevoEmailService.construirHtmlComprobante(
                "Paola Lopez", "REF-TEST-001", "$250.00", "5730094736", "Recarga", "TX-1002"
        );

        assertNotNull(html);
        assertTrue(html.contains("Paola Lopez"));
        assertTrue(html.contains("5730094736"));
        assertTrue(html.contains("Recarga"));
        assertTrue(html.contains("TX-1002"));
    }

    @Test
    @DisplayName("Debe obtener la API key por defecto si no está configurada")
    void testGetEffectiveApiKey() {
        ReflectionTestUtils.setField(brevoEmailService, "apiKey", null);
        String key = brevoEmailService.getEffectiveApiKey();
        assertNotNull(key);
        assertTrue(key.startsWith("xkeysib-"));

        ReflectionTestUtils.setField(brevoEmailService, "apiKey", "mi-custom-api-key");
        assertEquals("mi-custom-api-key", brevoEmailService.getEffectiveApiKey());
    }

    @Test
    @DisplayName("Debe retornar false si destinatario es nulo o vacío")
    void testDestinatarioNulo() {
        assertFalse(brevoEmailService.enviarCodigo2FA(null, "Paola", "123456", new BigDecimal("100.00"), "REF-1"));
        assertFalse(brevoEmailService.enviarCorreo("   ", "Paola", "Asunto", "<p>Html</p>", "Texto"));
    }

    @Test
    @DisplayName("Debe enviar código 2FA vía cliente Brevo")
    void testEnviarCodigo2FA_Exitoso() {
        BrevoEmailResponse resp = new BrevoEmailResponse();
        resp.setMessageId("<test-msg-id@brevo.com>");
        when(brevoEmailClient.sendEmail(anyString(), any())).thenReturn(resp);

        boolean enviado = brevoEmailService.enviarCodigo2FA("test@gmail.com", "Paola", "123456", new BigDecimal("250.00"), "REF-1");
        assertTrue(enviado);
    }

    @Test
    @DisplayName("Debe enviar comprobante de pago vía cliente Brevo")
    void testEnviarComprobantePago_Exitoso() {
        BrevoEmailResponse resp = new BrevoEmailResponse();
        resp.setMessageId("<test-msg-id-comprobante@brevo.com>");
        when(brevoEmailClient.sendEmail(anyString(), any())).thenReturn(resp);

        boolean enviado = brevoEmailService.enviarComprobantePago("test@gmail.com", "Paola", "REF-1", new BigDecimal("250.00"), "12345", "Prod", "TX-1", Map.of());
        assertTrue(enviado);
    }

    @Test
    @DisplayName("Debe manejar excepción de Feign y fallback")
    void testFeignExceptionFallback() {
        when(brevoEmailClient.sendEmail(anyString(), any())).thenThrow(new RuntimeException("Feign network error"));

        boolean enviado = brevoEmailService.enviarCodigo2FA("test@gmail.com", "Paola", "123456", new BigDecimal("250.00"), "REF-1");
        assertFalse(enviado);
    }

    @Test
    @DisplayName("Debe ejecutar intento por EmailJS cuando los parámetros están configurados")
    void testEnviarPorEmailJs_Configurado() {
        ReflectionTestUtils.setField(brevoEmailService, "emailjsServiceId", "service_test");
        ReflectionTestUtils.setField(brevoEmailService, "emailjsTemplateId", "template_test");
        ReflectionTestUtils.setField(brevoEmailService, "emailjsPublicKey", "key_test");

        // Al no haber red real hacia EmailJS, capturará la excepción internamente y continuará al fallback sin fallar
        boolean enviado = brevoEmailService.enviarCorreo("test@gmail.com", "Paola", "Asunto", "<p>Hola</p>", "Texto");
        assertFalse(enviado);
    }
}
