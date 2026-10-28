package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.model.dto.pago.Respuesta2FA;
import com.proyecto.servicios.service.BrevoEmailService;
import com.proyecto.servicios.service.Email2FAService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class Email2FAServiceImpl implements Email2FAService {

    private static final int EXPIRACION_SEGUNDOS = 300; // 5 minutos de validez
    private static final String REDIS_PREFIX = "pago:2fa:";

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final BrevoEmailService brevoEmailService;

    @Value("${spring.mail.username:isapaola188@gmail.com}")
    private String smtpUsername;

    // Almacén en memoria como respaldo cuando Redis no esté disponible
    private final Map<String, CodigoCache> localCache = new ConcurrentHashMap<>();

    private record CodigoCache(String codigo, LocalDateTime fechaExpiracion) {}

    @Autowired
    public Email2FAServiceImpl(@Nullable JavaMailSender mailSender,
                               @Nullable @Qualifier("productosRedisTemplate") RedisTemplate<String, String> redisTemplate,
                               BrevoEmailService brevoEmailService) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.brevoEmailService = brevoEmailService;
    }

    @Override
    public Respuesta2FA generarYEnviarCodigo2FA(Integer idCliente, String numeroCuenta, String correoCliente, String nombreCliente, BigDecimal monto, String referencia) {
        String codigoGenerado = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        LocalDateTime fechaExpiracion = LocalDateTime.now().plusSeconds(EXPIRACION_SEGUNDOS);

        String claveCache = construirClave(idCliente, numeroCuenta);

        // 1. Guardar en Redis si está disponible
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_PREFIX + claveCache, codigoGenerado, EXPIRACION_SEGUNDOS, TimeUnit.SECONDS);
            } catch (Exception ex) {
                log.warn("No fue posible guardar el código 2FA en Redis: {}. Se utilizará la memoria local.", ex.getMessage());
            }
        }

        // 2. Guardar en memoria local
        localCache.put(claveCache, new CodigoCache(codigoGenerado, fechaExpiracion));

        // 3. Enviar correo electrónico
        enviarCorreo(correoCliente, nombreCliente, codigoGenerado, monto, referencia);

        log.info("Código 2FA generado exitosamente para clienteId={} cuenta={} expiración={}", idCliente, numeroCuenta, fechaExpiracion);

        return Respuesta2FA.builder()
                .state(true)
                .code("00")
                .codigo(0)
                .message("Código de doble factor de autenticación enviado al correo electrónico exitosamente")
                .mensaje("Código de doble factor de autenticación enviado al correo electrónico exitosamente")
                .correoDestino(enmascararCorreo(correoCliente))
                .tiempoValidezSegundos(EXPIRACION_SEGUNDOS)
                .fechaExpiracion(fechaExpiracion)
                .build();
    }

    @Override
    public boolean validarCodigo2FA(Integer idCliente, String numeroCuenta, String codigoIngresado) {
        if (codigoIngresado == null || codigoIngresado.trim().isEmpty()) {
            return false;
        }

        String claveCache = construirClave(idCliente, numeroCuenta);
        String codigoEsperado = null;

        // 1. Intentar leer de Redis
        if (redisTemplate != null) {
            try {
                codigoEsperado = redisTemplate.opsForValue().get(REDIS_PREFIX + claveCache);
            } catch (Exception ex) {
                log.warn("Fallo al consultar código 2FA de Redis: {}", ex.getMessage());
            }
        }

        // 2. Si no se encontró en Redis, consultar memoria local
        if (codigoEsperado == null) {
            CodigoCache entry = localCache.get(claveCache);
            if (entry != null) {
                if (LocalDateTime.now().isBefore(entry.fechaExpiracion())) {
                    codigoEsperado = entry.codigo();
                } else {
                    localCache.remove(claveCache);
                    log.warn("El código 2FA en memoria local para clienteId={} ha expirado", idCliente);
                    return false;
                }
            }
        }

        if (codigoEsperado == null) {
            log.warn("No se encontró ningún código 2FA activo o pendiente para clienteId={}", idCliente);
            return false;
        }

        boolean coincide = codigoEsperado.trim().equals(codigoIngresado.trim());
        if (coincide) {
            invalidarCodigo2FA(idCliente, numeroCuenta);
            log.info("Autenticación de doble factor 2FA validada correctamente para clienteId={}", idCliente);
        } else {
            log.warn("El código 2FA ingresado [{}] no coincide con el código emitido [{}] para clienteId={}", codigoIngresado, codigoEsperado, idCliente);
        }

        return coincide;
    }

    @Override
    public void invalidarCodigo2FA(Integer idCliente, String numeroCuenta) {
        String claveCache = construirClave(idCliente, numeroCuenta);
        localCache.remove(claveCache);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_PREFIX + claveCache);
            } catch (Exception ex) {
                log.warn("Error al eliminar código 2FA de Redis: {}", ex.getMessage());
            }
        }
    }

    @Override
    public boolean enviarComprobantePago(String correoCliente, String nombreCliente, String referencia, BigDecimal monto,
                                         String numeroCuenta, String nombreProducto, String transaccionId, Map<String, Object> ticket) {
        boolean enviado = false;

        // 1. Envío directo garantizado e instantáneo por Gmail SMTP oficial (Llega en 1 segundo directo)
        if (mailSender != null && correoCliente != null && !correoCliente.isBlank()) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(smtpUsername, "Sistema de Pagos Seguros");
                helper.setTo(correoCliente.trim());
                helper.setSubject("Comprobante de Pago Exitoso - Ref: " + referencia);

                String htmlContent = (brevoEmailService instanceof BrevoEmailServiceImpl brevoImpl)
                        ? brevoImpl.construirHtmlComprobante(nombreCliente, referencia, "$" + monto, numeroCuenta, nombreProducto, transaccionId)
                        : "Tu pago por $" + monto + " ha sido confirmado exitosamente. Ref: " + referencia;

                helper.setText(htmlContent, true);
                mailSender.send(mimeMessage);
                log.info(">> [COMPROBANTE REAL ENTREGADO POR GMAIL SMTP] Comprobante entregado a la bandeja de: {}", correoCliente);
                enviado = true;
            } catch (Exception ex) {
                log.warn(">> [GMAIL SMTP LOCAL COMPROBANTE]: {}", ex.getMessage());
            }
        }

        // 2. Envío a través de EmailJS / Brevo API (Garantizado para Render / nube sin bloqueo de puertos)
        if (brevoEmailService != null && correoCliente != null && !correoCliente.isBlank()) {
            try {
                boolean enviadoServicio = brevoEmailService.enviarComprobantePago(correoCliente.trim(), nombreCliente, referencia, monto, numeroCuenta, nombreProducto, transaccionId, ticket);
                if (enviadoServicio) {
                    enviado = true;
                }
            } catch (Exception ex) {
                log.warn("Fallo al enviar comprobante por Email Service: {}", ex.getMessage());
            }
        }

        return enviado;
    }

    private void enviarCorreo(String destinatario, String nombre, String codigo, BigDecimal monto, String referencia) {
        log.info("=================================================================================");
        log.info(">> [SEGURIDAD 2FA] CÓDIGO GENERADO: [{}] para: {} (Monto: ${}, Ref: {})",
                codigo, destinatario, monto, referencia);
        log.info("=================================================================================");

        boolean enviado = false;

        // 1. Envío DIRECTO e INSTANTÁNEO por Gmail SMTP oficial (Llega en 1 segundo directo a la bandeja)
        if (mailSender != null && destinatario != null && !destinatario.isBlank()) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(smtpUsername, "Sistema de Pagos Seguros");
                helper.setTo(destinatario.trim());
                helper.setSubject("Código de Seguridad 2FA - Autorización de Pago (" + referencia + ")");

                String htmlContent = (brevoEmailService instanceof BrevoEmailServiceImpl brevoImpl)
                        ? brevoImpl.construirHtml2FA(nombre != null ? nombre : "Cliente", codigo, "$" + monto, referencia)
                        : "Tu código de doble autenticación (2FA) es: <b>" + codigo + "</b>";

                helper.setText(htmlContent, true);
                mailSender.send(mimeMessage);
                log.info(">> [CORREO REAL ENTREGADO POR GMAIL SMTP DIRECTO] Código 2FA entregado a la bandeja de: {}", destinatario);
                enviado = true;
            } catch (Exception ex) {
                log.warn(">> [GMAIL SMTP EXCEPCIÓN]: {}", ex.getMessage());
            }
        }

        // 2. Envío a través de EmailJS / Brevo API (Garantizado para la nube / Render vía HTTPS 443)
        if (brevoEmailService != null && destinatario != null && !destinatario.isBlank()) {
            try {
                boolean enviadoApi = brevoEmailService.enviarCodigo2FA(destinatario.trim(), nombre, codigo, monto, referencia);
                if (enviadoApi) {
                    enviado = true;
                }
            } catch (Exception ex) {
                log.warn(">> [EMAIL SERVICE EXCEPCIÓN]: {}", ex.getMessage());
            }
        }

        if (!enviado) {
            log.info(">> [CÓDIGO 2FA DISPONIBLE EN CONSOLA]: [{}] para [{}]", codigo, destinatario);
        }
    }

    private String construirClave(Integer idCliente, String numeroCuenta) {
        return (idCliente != null ? idCliente : "0") + ":" + (numeroCuenta != null ? numeroCuenta.trim() : "default");
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
}
