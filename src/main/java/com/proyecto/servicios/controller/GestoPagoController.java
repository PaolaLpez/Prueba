package com.proyecto.servicios.controller;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoEchoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

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
    public ResponseEntity<GestoPagoEchoResponse> sendEcho() {
        String token = Optional.ofNullable(gestoPagoTokenService
                        .obtenerTokenActivo(idDistribuidor, codigoDispositivo))
                .flatMap(value -> value)
                .map(GestoPagoToken::getToken)
                .orElseGet(() -> {
                    GestoPagoAuthResponse auth = gestoPagoAuthClient.authenticate(
                            apiKey, idDistribuidor, codigoDispositivo, password);
                    return auth == null ? null : auth.getToken();
                });

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No se pudo obtener un token de GestoPago");
        }

        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        GestoPagoEchoResponse response = parsearEcho(gestoPagoAuthClient.sendEcho(authorizationHeader));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private GestoPagoEchoResponse parsearEcho(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            String echo = document.getElementsByTagName("ECHO").item(0).getTextContent().trim();
            String valid = document.getElementsByTagName("VALID").item(0).getTextContent().trim();
            return GestoPagoEchoResponse.builder()
                    .codigo("1".equals(valid) ? 0 : 1)
                    .mensaje("1".equals(valid) ? "Conectividad validada" : "Conectividad no validada")
                    .status(valid)
                    .echo(echo)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Respuesta XML inválida de sendEcho.do", ex);
        }
    }

    // Endpoint POST para forzar la renovación del token GestoPago
    @PostMapping(value = "/renovar-token")
    public ResponseEntity<String> renovarToken() {
        gestoPagoTokenService.renovarToken();
        return new ResponseEntity<>("Token de GestoPago renovado exitosamente", HttpStatus.OK);
    }
}
