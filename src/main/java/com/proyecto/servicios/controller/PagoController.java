package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.error.ApiErrorResponse;
import com.proyecto.servicios.model.dto.pago.PagoConfirmacionResponse;
import com.proyecto.servicios.model.dto.pago.PagoRequest;
import com.proyecto.servicios.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para procesar pagos y compras seguras con Gestopago y Brevo 2FA.
 */
@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Transacciones y Pagos Seguros", description = "Endpoints de procesamiento transaccional, autenticación 2FA por correo electrónico con Brevo API e integración con GestoPago (POST /sistema/service/sendTx.do)")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * Endpoint transaccional único POST para procesar compras y pagos:
     * - Fase 1 (dobleAutenticacion = null): Valida cuenta, cliente, catálogo y saldo; genera y envía código 2FA al correo con Brevo API.
     * - Fase 2 (dobleAutenticacion = OTP): Valida 2FA, reserva saldo con bloqueo pesimista (Saga en 2 fases), consume GestoPago sendTx.do con Bearer Token, consolida saldo y envía comprobante digital por correo con Brevo.
     */
    @Operation(
            summary = "Procesar y confirmar compra / pago transaccional",
            description = "Flujo transaccional robusto en 2 fases:\n\n" +
                    "1. **Fase de Desafío 2FA**: Envía la petición con `dobleAutenticacion: null`. El sistema valida la cuenta activa, la pertenencia al cliente, la existencia en catálogo y consistencia de montos del producto, y despacha un código OTP de 6 dígitos al correo electrónico registrado vía la API de Brevo.\n\n" +
                    "2. **Fase de Confirmación**: Envía la misma petición agregando el código recibido en `dobleAutenticacion`. El sistema valida el 2FA, aplica bloqueo pesimista de saldo con reserva preventiva, consume `POST https://gestopago.portalventas.net/sistema/service/sendTx.do` con autenticación Bearer, actualiza el saldo a nivel definitivo y envía el comprobante digital al correo del cliente con Brevo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Operación procesada exitosamente (Código 2FA enviado o Pago confirmado con Gestopago)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagoConfirmacionResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Ejemplo 2FA Enviado (Fase 1)",
                                            value = "{\"state\":true,\"code\":\"01\",\"codigo\":1,\"message\":\"Código de autenticación (2FA) enviado a tu correo electrónico: lopezpao869@gmail.com.\",\"data\":{\"numeroCuenta\":\"5730094736\",\"idCliente\":3,\"nombreCliente\":\"Paola Lopez\",\"correoCliente\":\"lopezpao869@gmail.com\",\"idProducto\":\"1\",\"monto\":250.00,\"estadoTransaccion\":\"PENDIENTE_CODIGO_2FA\"}}"
                                    ),
                                    @ExampleObject(
                                            name = "Ejemplo Pago Exitoso (Fase 2)",
                                            value = "{\"state\":true,\"code\":\"00\",\"codigo\":0,\"message\":\"Transacción exitosa. Pago confirmado y procesado con Gestopago\",\"data\":{\"transaccionId\":\"12\",\"numeroCuenta\":\"5730094736\",\"idCliente\":3,\"nombreCliente\":\"Paola Lopez\",\"monto\":250.00,\"saldoAnterior\":2500.00,\"saldoDisponibleActual\":2250.00,\"estadoTransaccion\":\"EXITOSA\"}}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud inválida (parámetros faltantes, tipos incompatibles o formato incorrecto)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Recurso no encontrado (Cliente inexistente, cuenta no localizada o producto no existe en el catálogo)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto o duplicidad (Transacción con la misma referencia ya ejecutada previamente)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Violación de regla de negocio (Saldo insuficiente, monto no coincide con precio oficial del catálogo, producto no disponible, cuenta inactiva o código 2FA incorrecto/expirado)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "504",
                    description = "Tiempo de espera agotado con el proveedor externo Gestopago (Operación retenida en estado PENDIENTE_CONCILIACION para proteger el saldo)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoConfirmacionResponse> procesarPago(@Valid @RequestBody PagoRequest request) {
        PagoConfirmacionResponse response = pagoService.procesarPago(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
