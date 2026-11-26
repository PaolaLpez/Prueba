package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.pago.Respuesta2FA;
import com.proyecto.servicios.service.Impl.BrevoEmailServiceImpl;
import com.proyecto.servicios.service.Impl.Email2FAServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Email2FAServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BrevoEmailServiceImpl brevoEmailService;

    private Email2FAServiceImpl email2FAService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        email2FAService = new Email2FAServiceImpl(mailSender, redisTemplate, brevoEmailService);
        ReflectionTestUtils.setField(email2FAService, "smtpUsername", "isapaola188@gmail.com");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Debe generar y enviar código 2FA exitosamente")
    void testGenerarYEnviarCodigo2FA() {
        Respuesta2FA respuesta = email2FAService.generarYEnviarCodigo2FA(
                1, "1234567890", "test@correo.com", "Paola Lopez", new BigDecimal("250.00"), "REF-100"
        );

        assertNotNull(respuesta);
        assertTrue(respuesta.getState());
        assertEquals("00", respuesta.getCode());
        assertNotNull(respuesta.getFechaExpiracion());
        verify(redisTemplate.opsForValue(), times(1)).set(anyString(), anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Debe validar código 2FA exitosamente cuando coincide en memoria/redis")
    void testValidarCodigo2FA_Exitoso() {
        when(valueOperations.get("pago:2fa:1:1234567890")).thenReturn("654321");

        boolean valido = email2FAService.validarCodigo2FA(1, "1234567890", "654321");

        assertTrue(valido);
        verify(redisTemplate, times(1)).delete("pago:2fa:1:1234567890");
    }

    @Test
    @DisplayName("Debe retornar false si el código ingresado es null o vacío")
    void testValidarCodigo2FA_CodigoVacio() {
        assertFalse(email2FAService.validarCodigo2FA(1, "1234567890", null));
        assertFalse(email2FAService.validarCodigo2FA(1, "1234567890", "   "));
    }

    @Test
    @DisplayName("Debe retornar false cuando el código ingresado no coincide")
    void testValidarCodigo2FA_CodigoErroneo() {
        when(valueOperations.get("pago:2fa:1:1234567890")).thenReturn("654321");

        boolean valido = email2FAService.validarCodigo2FA(1, "1234567890", "000000");

        assertFalse(valido);
    }

    @Test
    @DisplayName("Debe invalidar código 2FA eliminando de Redis y memoria")
    void testInvalidarCodigo2FA() {
        email2FAService.invalidarCodigo2FA(1, "1234567890");

        verify(redisTemplate, times(1)).delete("pago:2fa:1:1234567890");
    }

    @Test
    @DisplayName("Debe enviar comprobante de pago por correo y brevo")
    void testEnviarComprobantePago() {
        when(brevoEmailService.construirHtmlComprobante(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<html>Comprobante</html>");
        when(brevoEmailService.enviarComprobantePago(anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);

        boolean enviado = email2FAService.enviarComprobantePago(
                "cliente@correo.com", "Paola", "REF-001", new BigDecimal("250.00"),
                "1234567890", "Recarga", "TX-1", Map.of("key", "val")
        );

        assertTrue(enviado);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Debe validar usando localCache cuando Redis falla o no tiene el valor")
    void testValidarCodigo2FA_LocalCacheFallback() {
        // Generar código con Redis fallando
        doThrow(new RuntimeException("Redis connection error")).when(valueOperations)
                .set(anyString(), anyString(), anyLong(), any());

        Respuesta2FA resp = email2FAService.generarYEnviarCodigo2FA(
                2, "9876543210", "local@correo.com", "Cliente Local", new BigDecimal("100.00"), "REF-LOCAL"
        );
        assertNotNull(resp);

        // Al validar, simular que Redis retorna null para que consulte localCache
        when(valueOperations.get("pago:2fa:2:9876543210")).thenReturn(null);

        // Validar con código erróneo
        assertFalse(email2FAService.validarCodigo2FA(2, "9876543210", "000000"));
    }
}

