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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
            Document document = null;
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                document = factory.newDocumentBuilder().parse(
                        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception exDoc) {
                log.warn(">> [GESTOPAGO] XML no estructurado estrictamente, se usará extractor por expresiones regulares: {}", exDoc.getMessage());
            }

            // Extracción insensible a mayúsculas/minúsculas y compatible con atributos y elementos
            String valid = buscarValorXml(document, xml, "VALID", "valid", "valido");
            String codigoStr = buscarValorXml(document, xml, "CODIGO", "codigo", "code", "CODE", "error", "ERROR", "codigoError");
            String status = buscarValorXml(document, xml, "STATUS", "status", "estatus", "estado");
            String mensaje = buscarValorXml(document, xml, "MENSAJE", "mensaje", "message", "MESSAGE", "descripcion", "DESCRIPCION", "motivo", "detalle");
            String folio = buscarValorXml(document, xml, "FOLIO", "folio", "Folio", "idTransaccion", "folioProveedor", "FOLIO_PROVEEDOR", "folioGestoPago", "noTransaccion", "transaccion");
            String autorizacion = buscarValorXml(document, xml, "AUTORIZACION", "autorizacion", "Autorizacion", "auth", "AUTH", "noAutorizacion", "numeroAutorizacion");
            String fecha = buscarValorXml(document, xml, "FECHA", "fecha", "date");
            String hora = buscarValorXml(document, xml, "HORA", "hora", "time");

            int codigoInt = 0;
            if (codigoStr != null && !codigoStr.isBlank()) {
                try {
                    codigoInt = Integer.parseInt(codigoStr.trim());
                } catch (Exception ignored) {
                    codigoInt = -1;
                }
            }

            // Detección rigurosa de errores y rechazos (evita falsos positivos como el error -103 del carrier)
            boolean tieneErrorExplicito = false;
            if (codigoInt != 0) {
                tieneErrorExplicito = true;
            }
            if ("0".equals(valid) || "false".equalsIgnoreCase(valid) || "0".equals(status) || "ERROR".equalsIgnoreCase(status) || "RECHAZADA".equalsIgnoreCase(status) || "RECHAZADO".equalsIgnoreCase(status)) {
                tieneErrorExplicito = true;
            }
            // Si la autorización o mensaje contiene códigos negativos o fallos de carrier (ej. "-103Por el momento el carrier no responde...")
            if (autorizacion != null && (autorizacion.startsWith("-") || autorizacion.toLowerCase().contains("carrier") || autorizacion.toLowerCase().contains("error") || autorizacion.toLowerCase().contains("no responde"))) {
                tieneErrorExplicito = true;
                if (mensaje == null || mensaje.isBlank() || "Transacción procesada".equalsIgnoreCase(mensaje)) {
                    mensaje = autorizacion.trim();
                }
            }
            if (mensaje != null && (mensaje.toLowerCase().contains("carrier no responde")
                    || mensaje.toLowerCase().contains("saldo insuficiente")
                    || mensaje.toLowerCase().contains("rechazad")
                    || mensaje.toLowerCase().contains("no disponible")
                    || mensaje.toLowerCase().contains("no responde")
                    || mensaje.toLowerCase().contains("favor de intentar"))) {
                tieneErrorExplicito = true;
            }

            boolean esAprobada = !tieneErrorExplicito
                    && (codigoInt == 0)
                    && ("1".equals(valid) || "true".equalsIgnoreCase(valid) || "EXITOSA".equalsIgnoreCase(status) || "APROBADA".equalsIgnoreCase(status) || (valid == null && status == null));

            if (!esAprobada && (mensaje == null || mensaje.isBlank() || "Transacción procesada".equalsIgnoreCase(mensaje))) {
                mensaje = (codigoInt != 0)
                        ? "Transacción rechazada por el proveedor (Código " + codigoInt + ")"
                        : "Operación rechazada por el proveedor GestoPago";
            }

            log.info(">> [GESTOPAGO PARSED REAL] Aprobada={}, Codigo={}, Folio={}, Auth={}, Msg={}",
                    esAprobada, codigoInt, folio, autorizacion, mensaje);

            return GestoPagoTxResponse.builder()
                    .exitoso(esAprobada)
                    .timeout(false)
                    .codigo(codigoInt)
                    .status(esAprobada ? "EXITOSA" : "RECHAZADA")
                    .mensaje(mensaje)
                    .folio(folio) // Folio real retornado por GestoPago (null si el proveedor no lo generó)
                    .autorizacion(esAprobada ? autorizacion : (autorizacion != null && !autorizacion.startsWith("-") ? autorizacion : "RECHAZADA"))
                    .fecha(fecha != null ? fecha : LocalDateTime.now().toLocalDate().toString())
                    .hora(hora != null ? hora : LocalDateTime.now().toLocalTime().toString())
                    .rawXml(xml)
                    .fechaOperacion(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            log.error(">> [GESTOPAGO XML ERROR] Error al parsear XML de respuesta: {}", ex.getMessage());
            return GestoPagoTxResponse.builder()
                    .exitoso(false)
                    .timeout(false)
                    .codigo(500)
                    .mensaje("Respuesta con formato no reconocido del proveedor GestoPago: " + ex.getMessage())
                    .rawXml(xml)
                    .fechaOperacion(LocalDateTime.now())
                    .build();
        }
    }

    private String buscarValorXml(Document doc, String xml, String... tagNames) {
        // 1. Búsqueda en elementos y atributos XML si el documento fue parseado
        if (doc != null) {
            NodeList allNodes = doc.getElementsByTagName("*");
            for (int i = 0; i < allNodes.getLength(); i++) {
                Node node = allNodes.item(i);
                for (String tag : tagNames) {
                    if (node.getNodeName().equalsIgnoreCase(tag)) {
                        String text = node.getTextContent();
                        if (text != null && !text.isBlank()) {
                            return text.trim();
                        }
                    }
                    if (node instanceof Element el) {
                        if (el.hasAttribute(tag)) {
                            String val = el.getAttribute(tag);
                            if (val != null && !val.isBlank()) return val.trim();
                        }
                        if (el.hasAttribute(tag.toLowerCase())) {
                            String val = el.getAttribute(tag.toLowerCase());
                            if (val != null && !val.isBlank()) return val.trim();
                        }
                    }
                }
            }
        }

        // 2. Respaldo por expresiones regulares sobre el texto XML en crudo
        if (xml != null && !xml.isBlank()) {
            for (String tag : tagNames) {
                // Formato <tag>valor</tag>
                java.util.regex.Matcher mTag = java.util.regex.Pattern.compile(
                        "<(?i:" + java.util.regex.Pattern.quote(tag) + ")[^>]*>([^<]+)</(?i:" + java.util.regex.Pattern.quote(tag) + ")>",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                ).matcher(xml);
                if (mTag.find()) {
                    String val = mTag.group(1).trim();
                    if (!val.isBlank()) return val;
                }

                // Formato atributo tag="valor"
                java.util.regex.Matcher mAttr = java.util.regex.Pattern.compile(
                        "(?i:" + java.util.regex.Pattern.quote(tag) + ")\\s*=\\s*[\"']([^\"']+)[\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                ).matcher(xml);
                if (mAttr.find()) {
                    String val = mAttr.group(1).trim();
                    if (!val.isBlank()) return val;
                }
            }
        }

        return null;
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
