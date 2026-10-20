package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.dto.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Filtro de protección perimetral de alto rendimiento (Rate Limiting y Bulkhead).
 * Protege la aplicación contra ataques de denegación de servicio (DoS) y pruebas
 * masivas en JMeter que intenten saturar los hilos de Tomcat o el pool de base de datos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int requestsPerSecond;
    private final int burstCapacity;
    private final Semaphore bulkheadSemaphore;
    private final ObjectMapper objectMapper;

    // Almacenamiento concurrente en memoria de buckets por dirección IP
    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${app.security.rate-limiter.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limiter.requests-per-second:60}") int requestsPerSecond,
            @Value("${app.security.rate-limiter.burst-capacity:90}") int burstCapacity,
            @Value("${app.security.rate-limiter.max-concurrent-requests:200}") int maxConcurrentRequests,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.requestsPerSecond = requestsPerSecond;
        this.burstCapacity = burstCapacity;
        this.bulkheadSemaphore = new Semaphore(maxConcurrentRequests, true);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Si el limitador está desactivado o es petición OPTIONS (CORS preflight), permitir paso directo
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Excluir endpoints de documentación Swagger y monitoreo Actuator
        if (path.contains("/swagger-ui") || path.contains("/v3/api-docs") || path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Control por IP con Algoritmo Token Bucket (Escudo Anti-JMeter)
        String clientIp = extraerIpCliente(request);
        TokenBucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> new TokenBucket(burstCapacity, requestsPerSecond));

        if (!bucket.consumir()) {
            log.warn("Límite de tasa excedido para la IP {}. Bloqueando petición temporalmente.", clientIp);
            escribirRespuestaRechazo(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas peticiones consecutivas (HTTP 429). Por favor espere un momento antes de reintentar.", "1");
            return;
        }

        // 2. Control Bulkhead (Semáforo de peticiones activas simultáneas en el servidor)
        boolean recursoAdquirido = bulkheadSemaphore.tryAcquire();
        if (!recursoAdquirido) {
            log.warn("Capacidad máxima de concurrencia alcanzada en el servidor. Protegiendo recursos.");
            escribirRespuestaRechazo(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "El servidor se encuentra atendiendo su capacidad máxima de peticiones. Intente nuevamente en unos instantes.", "2");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            bulkheadSemaphore.release();
        }
    }

    private void escribirRespuestaRechazo(HttpServletResponse response, HttpStatus status, String mensaje, String retryAfter)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", retryAfter);

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String extraerIpCliente(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "desconocido";
    }

    // Limpieza programada cada 10 minutos de clientes inactivos para evitar saturación de memoria RAM
    @Scheduled(fixedRate = 600000)
    public void limpiarBucketsInactivos() {
        long ahora = System.currentTimeMillis();
        ipBuckets.entrySet().removeIf(entry -> ahora - entry.getValue().getUltimaRecarga() > 600000);
    }

    /**
     * Implementación interna ligera y concurrente de Token Bucket
     */
    private static class TokenBucket {
        private final int capacidad;
        private final double tasaPorMilisegundo;
        private double tokens;
        private long ultimaRecarga;

        public TokenBucket(int capacidad, int tokensPorSegundo) {
            this.capacidad = capacidad;
            this.tasaPorMilisegundo = (double) tokensPorSegundo / 1000.0;
            this.tokens = capacidad;
            this.ultimaRecarga = System.currentTimeMillis();
        }

        public synchronized boolean consumir() {
            long ahora = System.currentTimeMillis();
            long transcurrido = ahora - ultimaRecarga;
            ultimaRecarga = ahora;

            // Recargar tokens en función del tiempo transcurrido
            tokens = Math.min(capacidad, tokens + (transcurrido * tasaPorMilisegundo));

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public synchronized long getUltimaRecarga() {
            return ultimaRecarga;
        }
    }
}
