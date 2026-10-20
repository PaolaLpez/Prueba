package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteBusquedaRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // 1. Registro de Cliente y creación automática de cuenta bancaria
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        return new ResponseEntity<>(clienteService.registrarCliente(request), HttpStatus.CREATED);
    }

    // 2. Consulta de todos los clientes
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarTodosClientes() {
        return new ResponseEntity<>(clienteService.consultarTodosClientes(), HttpStatus.OK);
    }

    // 3. Único endpoint de consulta dinámica combinando múltiples filtros vía Request DTO con @RequestBody (sin @RequestParam ni @PathVariable)
    @PostMapping(value = {"/buscar", "/consulta"}, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarClientesDinamico(@Valid @RequestBody(required = false) ClienteBusquedaRequest request) {
        if (request == null) {
            request = new ClienteBusquedaRequest();
        }
        return new ResponseEntity<>(clienteService.buscarClientesDinamico(request), HttpStatus.OK);
    }

    // 4. Actualización de cliente mediante Request DTO con @RequestBody (el ID viene dentro del DTO, sin @PathVariable)
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(@Valid @RequestBody ClienteActualizacionRequest request) {
        return new ResponseEntity<>(clienteService.actualizarCliente(request), HttpStatus.OK);
    }

    // 5. Actualización parcial de cliente (PATCH) mediante Request DTO con @RequestBody (el ID viene dentro del DTO, sin @PathVariable)
    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarParcialCliente(@Valid @RequestBody ClienteActualizacionRequest request) {
        return new ResponseEntity<>(clienteService.actualizarCliente(request), HttpStatus.OK);
    }
}
