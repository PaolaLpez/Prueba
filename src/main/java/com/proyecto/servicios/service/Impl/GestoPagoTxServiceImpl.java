package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.dto.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.GestoPagoTxService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTxServiceImpl implements GestoPagoTxService {

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

    public GestoPagoTxServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                  GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Override
    public GestoPagoTxResponse enviarTransaccion(Integer idServicio, Integer idProducto, BigDecimal monto,
                                                 String referencia, String upc, String unidad) {
        log.info(">> [GESTOPAGO SENDTX] Iniciando envío de transacción: idServicio={}, idProducto={}, monto={}, ref={}",
                idServicio, idProducto, monto, enmascararReferencia(referencia));

        // 1. Obtener Token Bearer seguro y enmascarado
        String token = obtenerBearerToken();
        if (token == null || token.isBlank()) {
            log.error(">> [GESTOPAGO ERROR] No fue posible obtener un Bearer Token válido para procesar sendTx.do");
            return GestoPagoTxResponse.builder()
                    .exitoso(false)
                    .timeout(false)
                    .codigo(401)
                    .mensaje("Error de autenticación con el proveedor de pagos (Bearer Token no disponible)")
                    .fechaOperacion(LocalDateTime.now())
                    .build();
        }

        String authorizationHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        log.info(">> [GESTOPAGO AUTH] Bearer Token activo verificado: {}", enmascararToken(token));

        // 2. Consumo de POST /sistema/service/sendTx.do
        try {
            String xmlResponse = gestoPagoAuthClient.sendTx(
                    authorizationHeader,
                    idDistribuidor,
                    codigoDispositivo,
                    idServicio,
                    idProducto,
                    monto,
                    referencia,
                    upc,
                    unidad
            );

            log.info(">> [GESTOPAGO SENDTX OK] Respuesta recibida del proveedor. Analizando contenido XML...");
            return parsearRespuestaTx(xmlResponse);

        } catch (FeignException.GatewayTimeout | RetryableException ex) {
            // Manejo de Timeout estricto: NO asumir fallo, reportar PENDIENTE DE CONCILIACIÓN
            log.error(">> [GESTOPAGO TIMEOUT CRÍTICO] Se agotó el tiempo de espera en sendTx.do ({}) para ref={}. Requiere conciliación.",
                    ex.getMessage(), enmascararReferencia(referencia));
            return GestoPagoTxResponse.builder()
                    .exitoso(false)
                    .timeout(true)
                    .codigo(504)
                    .mensaje("Tiempo de espera agotado con el proveedor externo. La transacción requiere conciliación antes de reintentar.")
                    .fechaOperacion(LocalDateTime.now())
                    .build();

        } catch (FeignException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                log.error(">> [GESTOPAGO TIMEOUT SOCKET] SocketTimeoutException para ref={}. No se asume fallo.", enmascararReferencia(referencia));
                return GestoPagoTxResponse.builder()
                        .exitoso(false)
                        .timeout(true)
                        .codigo(504)
                        .mensaje("Tiempo de espera agotado en la conexión con Gestopago. Operación pendiente de conciliación.")
                        .fechaOperacion(LocalDateTime.now())
                        .build();
            }

            log.error(">> [GESTOPAGO HTTP ERROR]: Status={} Mensaje={}", ex.status(), ex.getMessage());
            String respuestaError = ex.contentUTF8();
            if (respuestaError != null && (respuestaError.contains("<TRANSACCION>") || respuestaError.contains("<RESPONSE>"))) {
                return parsearRespuestaTx(respuestaError);
            }

            return GestoPagoTxResponse.builder()
                    .exitoso(false)
                    .timeout(false)
                    .codigo(ex.status())
                    .mensaje("Error del proveedor Gestopago al procesar la operación: " + (ex.status() == 401 ? "Credenciales no autorizadas" : "Servicio no disponible"))
                    .fechaOperacion(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            log.error(">> [GESTOPAGO EXCEPCIÓN INESPERADA]: {}", ex.getMessage(), ex);
            return GestoPagoTxResponse.builder()
                    .exitoso(false)
                    .timeout(false)
                    .codigo(500)
                    .mensaje("Fallo de comunicación interna al conectar con Gestopago: " + ex.getMessage())
                    .fechaOperacion(LocalDateTime.now())
                    .build();
        }
    }

    private String obtenerBearerToken() {
        return Optional.ofNullable(gestoPagoTokenService)
                .flatMap(s -> s.obtenerTokenActivo(idDistribuidor, codigoDispositivo))
                .map(GestoPagoToken::getToken)
                .orElseGet(() -> {
                    log.info(">> [GESTOPAGO] Solicitando nuevo token mediante autenticación directa...");
                    try {
                        GestoPagoAuthResponse auth = gestoPagoAuthClient.authenticate(apiKey, idDistribuidor, codigoDispositivo, password);
                        return auth != null ? auth.getToken() : null;
                    } catch (Exception ex) {
                        log.warn("Fallo al autenticar directamente con GestoPago: {}", ex.getMessage());
                        return null;
                    }
                });
    }

    private GestoPagoTxResponse parsearRespuestaTx(String xml) {
        if (xml == null || xml.isBlank()) {
            return GestoPagoTxResponse.builder()
                    .exitoso(false)
                    .codigo(99)
                    .mensaje("Respuesta vacía recibida del proveedor")
                    .fechaOperacion(LocalDateTime.now())
                    .build();
        }

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

            String valid = obtenerTag(document, "VALID", "1");
            String codigoStr = obtenerTag(document, "CODIGO", "0");
            String status = obtenerTag(document, "STATUS", valid);
            String mensaje = obtenerTag(document, "MENSAJE", "Transacción procesada");
            String folio = obtenerTag(document, "FOLIO", "GP-" + System.currentTimeMillis());
            String autorizacion = obtenerTag(document, "AUTORIZACION", "AUTH-" + System.currentTimeMillis() % 1000000);
            String fecha = obtenerTag(document, "FECHA", LocalDateTime.now().toLocalDate().toString());
            String hora = obtenerTag(document, "HORA", LocalDateTime.now().toLocalTime().toString());

            int codigoInt = 0;
            try {
                codigoInt = Integer.parseInt(codigoStr);
            } catch (Exception ignored) {}

            boolean esAprobada = (codigoInt == 0) && ("1".equals(valid) || "1".equalsIgnoreCase(status) || "EXITOSA".equalsIgnoreCase(status));

            log.info(">> [GESTOPAGO PARSED] Aprobada={}, Folio={}, Auth={}, Msg={}",
                    esAprobada, folio, autorizacion, mensaje);

            return GestoPagoTxResponse.builder()
                    .exitoso(esAprobada)
                    .timeout(false)
                    .codigo(codigoInt)
                    .status(status)
                    .mensaje(mensaje)
                    .folio(folio)
                    .autorizacion(autorizacion)
                    .fecha(fecha)
                    .hora(hora)
                    .rawXml(xml)
                    .fechaOperacion(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            log.error(">> [GESTOPAGO XML ERROR] Error al parsear XML de respuesta: {}", ex.getMessage());
            // Si la respuesta contenía confirmación en texto plano
            boolean pareceExitosa = xml.toLowerCase().contains("exitosa") || xml.toLowerCase().contains("éxito");
            return GestoPagoTxResponse.builder()
                    .exitoso(pareceExitosa)
                    .timeout(false)
                    .codigo(pareceExitosa ? 0 : 500)
                    .mensaje(pareceExitosa ? "Transacción aprobada por el proveedor" : "Respuesta con formato no reconocido")
                    .rawXml(xml)
                    .fechaOperacion(LocalDateTime.now())
                    .build();
        }
    }

    private String obtenerTag(Document doc, String tagName, String defaultValue) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
            String text = nodes.item(0).getTextContent();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        return defaultValue;
    }

    private String enmascararToken(String token) {
        if (token == null || token.length() <= 12) {
            return "******";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }

    private String enmascararReferencia(String ref) {
        if (ref == null || ref.length() <= 4) {
            return "****";
        }
        return ref.substring(0, 2) + "****" + ref.substring(ref.length() - 2);
    }
}
