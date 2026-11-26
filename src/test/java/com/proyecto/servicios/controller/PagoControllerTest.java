package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.pago.PagoConfirmacionResponse;
import com.proyecto.servicios.model.dto.pago.PagoRequest;
import com.proyecto.servicios.service.PagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private PagoController pagoController;

    @Test
    @DisplayName("Debe procesar y confirmar pago a través del controlador")
    void testProcesarPago() {
        PagoRequest request = PagoRequest.builder()
                .correo("paola@gmail.com")
                .monto(new BigDecimal("250.00"))
                .build();

        PagoConfirmacionResponse response = PagoConfirmacionResponse.builder()
                .state(true)
                .code("00")
                .mensaje("Pago procesado exitosamente")
                .build();

        when(pagoService.procesarPago(request)).thenReturn(response);

        ResponseEntity<PagoConfirmacionResponse> result = pagoController.procesarPago(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("00", result.getBody().getCode());
        verify(pagoService, times(1)).procesarPago(request);
    }
}

