package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.dto.producto.ProductoListResponse;
import com.proyecto.servicios.model.dto.producto.ProductoResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

// Implementación del servicio de negocio de productos con inyección de dependencias por constructor
@Service
@Slf4j
public class ProductoServiceImpl implements com.proyecto.servicios.service.ProductoService {

    private final ProductoClient productoClient;

    // Token inyectado dinámicamente desde la configuración de la aplicación (sin valores hardcodeados)
    @Value("${producto.service.token}")
    private String tokenConfig;

    public ProductoServiceImpl(ProductoClient productoClient) {
        this.productoClient = productoClient;
    }

    @Override
    public ProductoResponse obtenerProductos() {
        // Registro en logs del inicio de la invocación
        log.info("Iniciando consumo del servicio externo GET /sistema/service/getProductList.do");

        try {
            // Construcción del encabezado Bearer Token a partir de la configuración
            String authorizationHeader = "Bearer " + tokenConfig;

            // Invocación del cliente Feign externo
            ProductoListResponse apiResponse = productoClient.obtenerProductos(authorizationHeader);

            // Registro en logs de finalización exitosa
            log.info("Consumo del servicio externo finalizado exitosamente");

            return ProductoResponse.builder()
                    .codigo(0)
                    .mensaje("Lista de productos obtenida correctamente")
                    .datos(Optional.ofNullable(apiResponse)
                            .map(ProductoListResponse::getProductos)
                            .orElseGet(Collections::emptyList))
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
            log.error("Respuesta no exitosa del servicio externo. Estado HTTP: {}", ex.status());
            HttpStatus status = Optional.ofNullable(HttpStatus.resolve(ex.status())).orElse(HttpStatus.BAD_GATEWAY);
            throw new ExternalServiceException("Error al comunicarse con el servicio externo", ex, status);

        } catch (Exception ex) {
            // Log de error no controlado de integración sin exponer datos sensibles
            log.error("Error inesperado en la integración de productos: {}", ex.getMessage());
            throw new ExternalServiceException("Error interno al procesar la integración de productos", ex, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
