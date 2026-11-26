package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.producto.ProductoResponse;
import com.proyecto.servicios.service.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ProductoController productoController;

    @Test
    @DisplayName("Debe retornar lista de productos desde el controlador")
    void testObtenerProductos() {
        ProductoResponse mockResponse = ProductoResponse.builder().codigo(0).mensaje("OK").build();
        when(productoService.obtenerProductos()).thenReturn(mockResponse);

        ResponseEntity<ProductoResponse> response = productoController.obtenerProductos();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        verify(productoService, times(1)).obtenerProductos();
    }
}

