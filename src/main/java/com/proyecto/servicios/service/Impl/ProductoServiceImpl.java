package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.dto.producto.ProductoResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
                                com.proyecto.servicios.client.GestoPagoAuthClient gestoPagoAuthClient) {
        this.productoClient = productoClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoAuthClient = gestoPagoAuthClient;
    }

    @Override
    public ProductoResponse obtenerProductos() {
        // Registro en logs del inicio de la invocación
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

            // Registro en logs de finalización exitosa
            log.info("Consumo del servicio externo finalizado exitosamente");

            return ProductoResponse.builder()
                    .codigo(0)
                    .mensaje("Lista de productos obtenida correctamente")
                    .datos(productos)
                    .build();

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
