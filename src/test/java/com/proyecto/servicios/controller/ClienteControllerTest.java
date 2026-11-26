package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteBusquedaRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    @Test
    @DisplayName("Debe registrar cliente desde ClienteController")
    void testRegistrarCliente() {
        ClienteRegistroRequest request = new ClienteRegistroRequest();
        ClienteResponse response = ClienteResponse.builder().id(1).build();
        when(clienteService.registrarCliente(request)).thenReturn(response);

        ResponseEntity<ClienteResponse> result = clienteController.registrarCliente(request);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1, result.getBody().getId());
    }

    @Test
    @DisplayName("Debe consultar todos los clientes")
    void testConsultarTodosClientes() {
        when(clienteService.consultarTodosClientes()).thenReturn(List.of(ClienteResponse.builder().id(1).build()));

        ResponseEntity<List<ClienteResponse>> result = clienteController.consultarTodosClientes();

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("Debe buscar clientes dinámicamente")
    void testConsultarClientesDinamico() {
        ClienteBusquedaRequest request = new ClienteBusquedaRequest();
        when(clienteService.buscarClientesDinamico(any(ClienteBusquedaRequest.class))).thenReturn(List.of());

        ResponseEntity<List<ClienteResponse>> result = clienteController.consultarClientesDinamico(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("Debe actualizar cliente")
    void testActualizarCliente() {
        ClienteActualizacionRequest request = new ClienteActualizacionRequest();
        ClienteResponse response = ClienteResponse.builder().id(1).build();
        when(clienteService.actualizarCliente(request)).thenReturn(response);

        ResponseEntity<ClienteResponse> result = clienteController.actualizarCliente(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("Debe actualizar parcialmente cliente")
    void testActualizarParcialCliente() {
        ClienteActualizacionRequest request = new ClienteActualizacionRequest();
        ClienteResponse response = ClienteResponse.builder().id(1).build();
        when(clienteService.actualizarCliente(request)).thenReturn(response);

        ResponseEntity<ClienteResponse> result = clienteController.actualizarParcialCliente(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}

