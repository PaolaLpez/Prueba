package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private ObjectMapper objectMapper;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        filterChain = mock(FilterChain.class);
        filter = new RateLimitingFilter(true, 60, 90, 200, objectMapper);
    }

    @Test
    @DisplayName("Debe permitir el paso directo en peticiones OPTIONS (CORS preflight)")
    void testDoFilter_Options() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/pagos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Debe permitir el paso directo en endpoints de Swagger UI")
    void testDoFilter_Swagger() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Debe procesar peticiones válidas consumiendo el token bucket y extrayendo IP de headers")
    void testDoFilter_HeadersIp() throws Exception {
        // Con X-Forwarded-For
        MockHttpServletRequest reqXff = new MockHttpServletRequest("POST", "/api/v1/pagos");
        reqXff.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");
        MockHttpServletResponse respXff = new MockHttpServletResponse();
        filter.doFilter(reqXff, respXff, filterChain);
        assertEquals(200, respXff.getStatus());

        // Con X-Real-IP
        MockHttpServletRequest reqRealIp = new MockHttpServletRequest("POST", "/api/v1/pagos");
        reqRealIp.addHeader("X-Real-IP", "198.51.100.1");
        MockHttpServletResponse respRealIp = new MockHttpServletResponse();
        filter.doFilter(reqRealIp, respRealIp, filterChain);
        assertEquals(200, respRealIp.getStatus());
    }

    @Test
    @DisplayName("Debe rechazar con HTTP 429 cuando se excede el Token Bucket")
    void testDoFilter_TooManyRequests() throws Exception {
        RateLimitingFilter tightFilter = new RateLimitingFilter(true, 1, 1, 100, objectMapper);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/pagos");
        req.setRemoteAddr("10.0.0.1");

        // 1st request ok
        tightFilter.doFilter(req, new MockHttpServletResponse(), filterChain);

        // 2nd immediate request should exceed bucket (HTTP 429)
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        tightFilter.doFilter(req, resp2, filterChain);
        assertEquals(429, resp2.getStatus());
    }

    @Test
    @DisplayName("Debe rechazar con HTTP 503 cuando el Bulkhead está saturado")
    void testDoFilter_BulkheadExceeded() throws Exception {
        RateLimitingFilter zeroBulkheadFilter = new RateLimitingFilter(true, 100, 100, 0, objectMapper);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/pagos");
        req.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        zeroBulkheadFilter.doFilter(req, resp, filterChain);
        assertEquals(503, resp.getStatus());
    }

    @Test
    @DisplayName("Debe permitir peticiones cuando el limitador está desactivado")
    void testDoFilter_Desactivado() throws Exception {
        RateLimitingFilter filterDisabled = new RateLimitingFilter(false, 60, 90, 200, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/pagos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filterDisabled.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Debe ejecutar limpieza de buckets inactivos sin errores")
    void testLimpiarBucketsInactivos() {
        filter.limpiarBucketsInactivos();
    }
}
