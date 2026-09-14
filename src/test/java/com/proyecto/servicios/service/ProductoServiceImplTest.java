package com.proyecto.servicios.service;

import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.dto.producto.ProductoDto;
import com.proyecto.servicios.model.dto.producto.ProductoListResponse;
import com.proyecto.servicios.model.dto.producto.ProductoResponse;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Pruebas unitarias para la capa de servicio ProductoServiceImpl simulando escenarios exitosos y de error
@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoClient productoClient;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private static final String TEST_TOKEN = "dummy-bearer-token";
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        // Asignación de propiedad tokenConfig usando ReflectionTestUtils
        ReflectionTestUtils.setField(productoService, "tokenConfig", TEST_TOKEN);
        
        dummyRequest = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                new HashMap<>(),
                null,
                new RequestTemplate()
        );
    }

    @Test
    @DisplayName("Debe obtener la lista de productos exitosamente")
    void testObtenerProductosExitoso() {
        // Preparación de datos mock
        ProductoDto producto = ProductoDto.builder()
                .id("PROD-001")
                .nombre("Producto de Prueba")
                .precio(new BigDecimal("150.00"))
                .disponible(true)
                .build();

        ProductoListResponse responseMock = ProductoListResponse.builder()
                .codigo(0)
                .mensaje("OK")
                .productos(List.of(producto))
                .build();

        when(productoClient.obtenerProductos(eq("Bearer " + TEST_TOKEN))).thenReturn(responseMock);

        // Invocación del servicio
        ProductoResponse resultado = productoService.obtenerProductos();

        // Verificaciones
        assertNotNull(resultado);
        assertEquals(0, resultado.getCodigo());
        assertEquals(1, resultado.getDatos().size());
        assertEquals("PROD-001", resultado.getDatos().get(0).getId());
        verify(productoClient, times(1)).obtenerProductos("Bearer " + TEST_TOKEN);
    }

    @Test
    @DisplayName("Debe lanzar ExternalServiceException cuando ocurre un error de autenticación (401)")
    void testObtenerProductosErrorAutenticacion() {
        FeignException.Unauthorized unauthorizedException = new FeignException.Unauthorized(
                "Unauthorized", dummyRequest, null, null
        );

        when(productoClient.obtenerProductos(anyString())).thenThrow(unauthorizedException);

        ExternalServiceException ex = assertThrows(
                ExternalServiceException.class,
                () -> productoService.obtenerProductos()
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertTrue(ex.getMessage().contains("autenticación"));
    }

    @Test
    @DisplayName("Debe lanzar ExternalServiceException cuando ocurre un timeout de comunicación")
    void testObtenerProductosTimeout() {
        RetryableException timeoutException = new RetryableException(
                504, "Gateway Timeout", Request.HttpMethod.GET, 1000L, dummyRequest
        );

        when(productoClient.obtenerProductos(anyString())).thenThrow(timeoutException);

        ExternalServiceException ex = assertThrows(
                ExternalServiceException.class,
                () -> productoService.obtenerProductos()
        );

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
        assertTrue(ex.getMessage().contains("Tiempo de espera agotado"));
    }

    @Test
    @DisplayName("Debe lanzar ExternalServiceException ante una respuesta no exitosa (500)")
    void testObtenerProductosRespuestaNoExitosa() {
        FeignException.InternalServerError serverError = new FeignException.InternalServerError(
                "Internal Server Error", dummyRequest, null, null
        );

        when(productoClient.obtenerProductos(anyString())).thenThrow(serverError);

        ExternalServiceException ex = assertThrows(
                ExternalServiceException.class,
                () -> productoService.obtenerProductos()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }
}
