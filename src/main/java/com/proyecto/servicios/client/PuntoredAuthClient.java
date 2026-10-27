package com.proyecto.servicios.client;

import com.proyecto.servicios.model.dto.puntored.PuntoredAuthRequest;
import com.proyecto.servicios.model.dto.puntored.PuntoredAuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente OpenFeign para consumir el servicio de autenticación de Puntored según documentación:
 * POST https://services.preprodcxr.co/puntored/api/v1/auth
 */
@FeignClient(name = "puntoredAuthClient", url = "${puntored.auth.url:https://services.preprodcxr.co/puntored/api/v1}")
public interface PuntoredAuthClient {

    @PostMapping(value = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    PuntoredAuthResponse authenticate(@RequestBody PuntoredAuthRequest request);
}
