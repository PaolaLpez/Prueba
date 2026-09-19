package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

// Cliente Feign para consultar el endpoint externo GET /sistema/service/getProductList.do con Bearer Token
@FeignClient(name = "productoClient", url = "${producto.service.url:https://gestopago.portalventas.net}")
public interface ProductoClient {

        @GetMapping(value = "/sistema/service/getProductList.do", produces = {"text/xml", "application/xml"})
    String obtenerProductos(
            @RequestHeader("Authorization") String bearerToken
    );
}
