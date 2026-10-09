package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.dto.producto.ProductoResponse;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

// Implementación del servicio de negocio de productos con inyección de dependencias por constructor
@Service
@Slf4j
public class ProductoServiceImpl implements com.proyecto.servicios.service.ProductoService {

    private final ProductoClient productoClient;
    private final com.proyecto.servicios.service.GestoPagoTokenService gestoPagoTokenService;
    private final com.proyecto.servicios.client.GestoPagoAuthClient gestoPagoAuthClient;
    private final ProductoRepository productoRepository;
    private final RedisTemplate<String, String> productosRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${productos.cache.key:productos:actuales}")
    private String cacheKey;

    @Value("${gestopago.auth.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String apiKey;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:12345678}")
    private String password;

    public ProductoServiceImpl(ProductoClient productoClient,
                                com.proyecto.servicios.service.GestoPagoTokenService gestoPagoTokenService,
                                com.proyecto.servicios.client.GestoPagoAuthClient gestoPagoAuthClient,
                                ProductoRepository productoRepository,
                                RedisTemplate<String, String> productosRedisTemplate,
                                ObjectMapper objectMapper) {
        this.productoClient = productoClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.productoRepository = productoRepository;
        this.productosRedisTemplate = productosRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProductoResponse obtenerProductos() {
        List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos = leerCache();
        if (productos == null) {
            productos = leerBaseDeDatos();
        }
        if (productos != null && !productos.isEmpty()) {
            productos = normalizarYAcomodarProductos(productos);
            guardarEnCache(productos);
            return respuestaProductos(productos, "Lista de productos obtenida correctamente");
        }

        productos = consultarApi();
        productos = normalizarYAcomodarProductos(productos);
        persistir(productos);
        guardarEnCache(productos);
        return respuestaProductos(productos, "Lista de productos obtenida correctamente");
    }

    private List<com.proyecto.servicios.model.dto.producto.ProductoDto> normalizarYAcomodarProductos(
            List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos) {
        if (productos == null) {
            return java.util.Collections.emptyList();
        }
        return productos.stream()
                .peek(p -> {
                    if (p.getPrecio() == null) {
                        p.setPrecio(java.math.BigDecimal.ZERO);
                    }
                    if (p.getCategoria() == null || p.getCategoria().trim().isEmpty()) {
                        p.setCategoria("0");
                    }
                })
                .sorted(java.util.Comparator.comparing(p -> p.getPrecio() != null ? p.getPrecio() : java.math.BigDecimal.ZERO))
                .collect(java.util.stream.Collectors.toList());
    }

    @Scheduled(cron = "${productos.sincronizacion.cron:0 0 6 * * *}")
    public void sincronizarProductos() {
        log.info("Iniciando sincronización programada de productos");
        try {
            List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos = consultarApi();
            persistir(productos);
            limpiarCache();
            guardarEnCache(productos);
            log.info("Sincronización de productos finalizada. Productos actualizados: {}", productos.size());
        } catch (Exception ex) {
            log.error("No fue posible sincronizar productos: {}", ex.getMessage(), ex);
        }
    }

    private List<com.proyecto.servicios.model.dto.producto.ProductoDto> consultarApi() {
        log.info("Iniciando consumo del servicio externo GET /sistema/service/getProductList.do");

        try {
            // Obtención dinámica del token activo o solicitud directa a GestoPago
            String token = Optional.ofNullable(gestoPagoTokenService)
                    .flatMap(s -> s.obtenerTokenActivo(idDistribuidor, codigoDispositivo))
                    .map(com.proyecto.servicios.entity.gestopago.GestoPagoToken::getToken)
                    .orElseGet(() -> {
                        log.info("Obteniendo token fresco mediante autenticación directa con GestoPago...");
                        com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse auth =
                                gestoPagoAuthClient.authenticate(apiKey, idDistribuidor, codigoDispositivo, password);
                        return auth != null ? auth.getToken() : null;
                    });

            if (token == null) {
                throw new ExternalServiceException("No se pudo obtener un token de autenticación válido para GestoPago", HttpStatus.UNAUTHORIZED);
            }

            // Construcción del encabezado Bearer Token
            String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

            // La API documentada devuelve XML; Feign lo recibe como texto para evitar
            // que Spring intente convertir text/xml a Object.
            String apiResponse = productoClient.obtenerProductos(authorizationHeader);
            List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos = parsearProductos(apiResponse);

            log.info("Consumo del servicio externo finalizado exitosamente");
                return productos;

        } catch (FeignException.Unauthorized | FeignException.Forbidden ex) {
            // Log de error de autenticación omitiendo tokens o credenciales sensibles
            log.error("Error de autenticación al consumir servicio de productos. Estado HTTP: {}", ex.status());
            throw new ExternalServiceException("Error de autenticación con el servicio externo", ex, HttpStatus.UNAUTHORIZED);

        } catch (FeignException.GatewayTimeout | RetryableException ex) {
            // Log de error por timeout de comunicación
            log.error("Timeout de conexión al consumir servicio externo de productos: {}", ex.getMessage());
            throw new ExternalServiceException("Tiempo de espera agotado al conectar con el servicio externo", ex, HttpStatus.GATEWAY_TIMEOUT);

        } catch (FeignException ex) {
            // Log de error de respuesta no exitosa del cliente Feign
            String detalle = ex.contentUTF8() != null && !ex.contentUTF8().trim().isEmpty() ? ex.contentUTF8() : ex.getMessage();
            log.error("Respuesta no exitosa del servicio externo. Estado HTTP: {}. Detalle: {}", ex.status(), detalle, ex);
            HttpStatus status = HttpStatus.resolve(ex.status());
            HttpStatus finalStatus = (status != null && status.isError()) ? status : HttpStatus.BAD_GATEWAY;
            throw new ExternalServiceException("Error al comunicarse con el servicio externo: " + detalle, ex, finalStatus);

        } catch (Exception ex) {
            // Log de error no controlado de integración sin exponer datos sensibles
            log.error("Error inesperado en la integración de productos: {}", ex.getMessage());
            throw new ExternalServiceException("Error interno al procesar la integración de productos", ex, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ProductoResponse respuestaProductos(List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos,
                                                String mensaje) {
        return ProductoResponse.builder().codigo(0).mensaje(mensaje).datos(productos).build();
    }

    private List<com.proyecto.servicios.model.dto.producto.ProductoDto> leerCache() {
        if (productosRedisTemplate == null || objectMapper == null) {
            return null;
        }
        try {
            String json = productosRedisTemplate.opsForValue().get(cacheKey);
            return json == null ? null : objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception ex) {
            log.warn("Redis no disponible; se intentará consultar PostgreSQL: {}", ex.getMessage());
            return null;
        }
    }

    private void guardarEnCache(List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos) {
        if (productosRedisTemplate == null || objectMapper == null) {
            return;
        }
        try {
            productosRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(productos));
        } catch (Exception ex) {
            log.warn("No fue posible guardar productos en Redis; PostgreSQL conserva la información: {}", ex.getMessage());
        }
    }

    private void limpiarCache() {
        if (productosRedisTemplate == null) {
            return;
        }
        try {
            productosRedisTemplate.delete(cacheKey);
        } catch (Exception ex) {
            log.warn("No fue posible limpiar Redis durante la sincronización: {}", ex.getMessage());
        }
    }

    private List<com.proyecto.servicios.model.dto.producto.ProductoDto> leerBaseDeDatos() {
        if (productoRepository == null) {
            return null;
        }
        return productoRepository.findAll().stream().map(this::aDto).toList();
    }

    private void persistir(List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos) {
        if (productoRepository == null) {
            return;
        }
        List<Producto> entidades = productos.stream()
                .filter(producto -> producto.getId() != null && !producto.getId().isBlank())
                .map(this::aEntidad)
                .toList();
        productoRepository.deleteAllInBatch();
        productoRepository.saveAll(entidades);
    }

    private Producto aEntidad(com.proyecto.servicios.model.dto.producto.ProductoDto dto) {
        Producto entity = new Producto();
        entity.setId(dto.getId());
        entity.setNombre(dto.getNombre());
        entity.setPrecio(dto.getPrecio());
        entity.setCategoria(dto.getCategoria());
        entity.setDisponible(dto.getDisponible());
        entity.setIdServicio(dto.getIdServicio());
        entity.setIdProducto(dto.getIdProducto());
        entity.setIdCatTipoServicio(dto.getIdCatTipoServicio());
        entity.setTipoFront(dto.getTipoFront());
        entity.setHasDigitoVerificador(dto.getHasDigitoVerificador());
        entity.setShowAyuda(dto.getShowAyuda());
        entity.setTipoReferencia(dto.getTipoReferencia());
        return entity;
    }

    private com.proyecto.servicios.model.dto.producto.ProductoDto aDto(Producto entity) {
        return com.proyecto.servicios.model.dto.producto.ProductoDto.builder()
            .id(entity.getId()).nombre(entity.getNombre())
                .precio(entity.getPrecio()).categoria(entity.getCategoria()).disponible(entity.getDisponible())
                .idServicio(entity.getIdServicio()).idProducto(entity.getIdProducto())
                .idCatTipoServicio(entity.getIdCatTipoServicio()).tipoFront(entity.getTipoFront())
                .hasDigitoVerificador(entity.getHasDigitoVerificador()).showAyuda(entity.getShowAyuda())
                .tipoReferencia(entity.getTipoReferencia()).build();
    }

    private List<com.proyecto.servicios.model.dto.producto.ProductoDto> parsearProductos(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            NodeList nodes = document.getElementsByTagName("producto");
            List<com.proyecto.servicios.model.dto.producto.ProductoDto> productos = new ArrayList<>();

            for (int index = 0; index < nodes.getLength(); index++) {
                Element producto = (Element) nodes.item(index);
                String precio = producto.getAttribute("precio");
                productos.add(com.proyecto.servicios.model.dto.producto.ProductoDto.builder()
                        .id(producto.getAttribute("idProducto"))
                        .nombre(producto.getAttribute("producto"))
                        .categoria(producto.getAttribute("servicio"))
                        .precio(precio.isBlank() ? null : new java.math.BigDecimal(precio))
                        .disponible(true)
                        .idServicio(entero(producto, "idServicio"))
                        .idProducto(entero(producto, "idProducto"))
                        .idCatTipoServicio(entero(producto, "idCatTipoServicio"))
                        .tipoFront(entero(producto, "tipoFront"))
                        .hasDigitoVerificador(booleano(producto, "hasDigitoVerificador"))
                        .showAyuda(booleano(producto, "showAyuda"))
                        .tipoReferencia(producto.getAttribute("tipoReferencia"))
                        .build());
            }
            return productos;
        } catch (Exception ex) {
            throw new ExternalServiceException("Respuesta XML inválida del servicio de productos", ex,
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private Integer entero(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        return value.isBlank() ? null : Integer.valueOf(value);
    }

    private Boolean booleano(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        return value.isBlank() ? null : Boolean.valueOf(value);
    }
}
