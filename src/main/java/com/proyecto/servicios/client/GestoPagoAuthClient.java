package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; 
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gestoPagoAuth", url = "${gestopago.auth.url:https://gestopago.portalventas.net}")
public interface GestoPagoAuthClient {

    @PostMapping("/sistema/app/jwt-gp/authenticate/")
    GestoPagoAuthResponse authenticate(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam("idDistribuidor") Integer idDistribuidor,
            @RequestParam("codigoDispositivo") String codigoDispositivo,
            @RequestParam("password") String password
    );

    // Mapeo para la API sendEcho.do que recibe el Bearer Token en el encabezado Authorization
    @GetMapping(value = "/sistema/service/sendEcho.do", produces = {"text/xml", "application/xml"})
    String sendEcho(@RequestHeader("Authorization") String bearerToken);

    // Mapeo oficial para la API transaccional sendTx.do que procesa la compra/recarga con Bearer Token
    @PostMapping(value = "/sistema/service/sendTx.do", produces = {"text/xml", "application/xml"})
    String sendTx(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam("idDistribuidor") Integer idDistribuidor,
            @RequestParam("codigoDispositivo") String codigoDispositivo,
            @RequestParam("idServicio") Integer idServicio,
            @RequestParam("idProducto") Integer idProducto,
            @RequestParam("monto") java.math.BigDecimal monto,
            @RequestParam("referencia") String referencia,
            @RequestParam(value = "upc", required = false) String upc,
            @RequestParam(value = "unidad", required = false) String unidad
    );
}
