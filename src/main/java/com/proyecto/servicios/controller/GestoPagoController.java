package com.proyecto.servicios.controller;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoEchoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Controlador REST para exponer las peticiones de GestoPago (autenticación y echo) en Swagger UI
@RestController
@RequestMapping("/api/v1/gestopago")
public class GestoPagoController {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String apiKey;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password:12345678}")
    private String password;

    public GestoPagoController(GestoPagoAuthClient gestoPagoAuthClient, GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    // Endpoint POST para autenticar directamente en GestoPago
    @PostMapping(value = "/autenticar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GestoPagoAuthResponse> autenticar() {
        GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(apiKey, idDistribuidor, codigoDispositivo, password);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Endpoint GET para enviar la prueba de conectividad sendEcho a GestoPago
    @GetMapping(value = "/echo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GestoPagoEchoResponse> sendEcho(@RequestHeader("Authorization") String token) {
        GestoPagoEchoResponse response = gestoPagoAuthClient.sendEcho(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Endpoint POST para forzar la renovación del token GestoPago
    @PostMapping(value = "/renovar-token")
    public ResponseEntity<String> renovarToken() {
        gestoPagoTokenService.renovarToken();
        return new ResponseEntity<>("Token de GestoPago renovado exitosamente", HttpStatus.OK);
    }
}
