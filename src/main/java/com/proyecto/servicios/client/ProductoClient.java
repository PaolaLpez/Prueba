package com.proyecto.servicios.client;

import com.proyecto.servicios.model.dto.producto.ProductoListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

// Cliente Feign para consultar el endpoint externo GET /sistema/service/getProductList.do con Bearer Token
@FeignClient(name = "productoClient", url = "${producto.service.url}")
public interface ProductoClient {

    @GetMapping("/sistema/service/getProductList.do")
    ProductoListResponse obtenerProductos(@RequestHeader("Authorization") String bearerToken);
}
