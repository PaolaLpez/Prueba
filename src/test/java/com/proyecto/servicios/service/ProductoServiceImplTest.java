package com.proyecto.servicios.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.model.dto.producto.ProductoDto;
import com.proyecto.servicios.model.dto.producto.ProductoResponse;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoClient productoClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private RedisTemplate<String, String> productosRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productoService, "cacheKey", "productos:actuales");
        ReflectionTestUtils.setField(productoService, "apiKey", "test-key");
        ReflectionTestUtils.setField(productoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(productoService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(productoService, "password", "12345678");
        lenient().when(productosRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Debe obtener productos desde caché de Redis si están disponibles")
    void testObtenerProductos_DesdeCache() throws Exception {
        when(valueOperations.get("productos:actuales")).thenReturn("[{\"id\":\"1\",\"nombre\":\"Recarga\"}]");
        ProductoDto dto = ProductoDto.builder().id("1").nombre("Recarga").precio(new BigDecimal("100.00")).build();
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(dto));

        ProductoResponse response = productoService.obtenerProductos();

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
        assertTrue(response.getDatos() instanceof List);
        List<?> lista = (List<?>) response.getDatos();
        assertEquals(1, lista.size());
        verify(productoRepository, never()).findAll();
    }

    @Test
    @DisplayName("Debe obtener productos desde base de datos si no hay caché en Redis")
    void testObtenerProductos_DesdeBaseDeDatos() {
        when(valueOperations.get("productos:actuales")).thenReturn(null);

        Producto prod = new Producto();
        prod.setId("1");
        prod.setNombre("Recarga");
        prod.setPrecio(new BigDecimal("100.00"));
        when(productoRepository.findAll()).thenReturn(List.of(prod));

        ProductoResponse response = productoService.obtenerProductos();

        assertNotNull(response);
        assertTrue(response.getDatos() instanceof List);
        List<?> lista = (List<?>) response.getDatos();
        assertEquals(1, lista.size());
        ProductoDto primerDto = (ProductoDto) lista.get(0);
        assertEquals("Recarga", primerDto.getNombre());
    }

    @Test
    @DisplayName("Debe manejar fallos de Redis silenciosamente consultando base de datos")
    void testObtenerProductos_FalloRedisFallbackBD() {
        when(valueOperations.get("productos:actuales")).thenThrow(new RuntimeException("Redis down"));

        Producto prod = new Producto();
        prod.setId("2");
        prod.setNombre("Agua");
        prod.setPrecio(new BigDecimal("50.00"));
        when(productoRepository.findAll()).thenReturn(List.of(prod));

        ProductoResponse response = productoService.obtenerProductos();

        assertNotNull(response);
        assertTrue(response.getDatos() instanceof List);
        List<?> lista = (List<?>) response.getDatos();
        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("Debe sincronizar productos consultando la API externa en XML y persistiendo")
    void testSincronizarProductos_Exitoso() {
        com.proyecto.servicios.entity.gestopago.GestoPagoToken token = new com.proyecto.servicios.entity.gestopago.GestoPagoToken();
        token.setToken("TOKEN-PRODUCTOS");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(java.util.Optional.of(token));

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response>"
                + "<producto idProducto=\"1\" producto=\"Recarga Telefonia\" servicio=\"Telefonia\" precio=\"200.00\" "
                + "idServicio=\"1\" idCatTipoServicio=\"1\" tipoFront=\"1\" hasDigitoVerificador=\"false\" showAyuda=\"false\" tipoReferencia=\"NUM\"/>"
                + "</response>";
        when(productoClient.obtenerProductos(anyString())).thenReturn(xml);

        assertDoesNotThrow(() -> productoService.sincronizarProductos());

        verify(productoRepository, times(1)).deleteAllInBatch();
        verify(productoRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Debe autenticar directamente con GestoPago si no hay token en caché al consultar API")
    void testConsultarApi_AutenticacionDirecta() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());

        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(java.util.Optional.empty());
        com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse auth = new com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse();
        auth.setToken("TOKEN-DIRECTO");
        when(gestoPagoAuthClient.authenticate(anyString(), anyInt(), anyString(), anyString())).thenReturn(auth);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response>"
                + "<producto idProducto=\"2\" producto=\"Agua\" servicio=\"\" precio=\"\" "
                + "idServicio=\"1\" idCatTipoServicio=\"1\" tipoFront=\"1\" hasDigitoVerificador=\"\" showAyuda=\"\" tipoReferencia=\"\"/>"
                + "</response>";
        when(productoClient.obtenerProductos(anyString())).thenReturn(xml);

        ProductoResponse response = productoService.obtenerProductos();
        assertNotNull(response);
    }

    @Test
    @DisplayName("Debe lanzar ExternalServiceException si la respuesta XML de productos es inválida")
    void testConsultarApi_XmlInvalido() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(List.of());

        com.proyecto.servicios.entity.gestopago.GestoPagoToken token = new com.proyecto.servicios.entity.gestopago.GestoPagoToken();
        token.setToken("TOKEN");
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(java.util.Optional.of(token));
        when(productoClient.obtenerProductos(anyString())).thenReturn("NOT_VALID_XML");

        assertThrows(com.proyecto.servicios.exception.ExternalServiceException.class, () -> productoService.obtenerProductos());
    }
}
