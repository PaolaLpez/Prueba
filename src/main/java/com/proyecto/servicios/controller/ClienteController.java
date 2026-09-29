package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    // 3. Consulta de cliente por ID
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable("id") Integer id) {
        return new ResponseEntity<>(clienteService.consultarPorId(id), HttpStatus.OK);
    }

    // 4. Consulta de cliente por CURP
    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorCurp(@PathVariable("curp") String curp) {
        return new ResponseEntity<>(clienteService.consultarPorCurp(curp), HttpStatus.OK);
    }

    // 5. Consulta de cliente por RFC
    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorRfc(@PathVariable("rfc") String rfc) {
        return new ResponseEntity<>(clienteService.consultarPorRfc(rfc), HttpStatus.OK);
    }

    // 6. Consulta de cliente por Correo Electrónico
    @GetMapping(value = "/correo/{correo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorCorreo(@PathVariable("correo") String correo) {
        return new ResponseEntity<>(clienteService.consultarPorCorreo(correo), HttpStatus.OK);
    }

    // 7. Consulta de clientes por rango de fechas de registro
    @GetMapping(value = "/rango-fechas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarPorRangoFechas(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return new ResponseEntity<>(clienteService.consultarClientesPorRangoFechas(inicio, fin), HttpStatus.OK);
    }

    // 8. Consulta de clientes activos
    @GetMapping(value = "/activos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarClientesActivos() {
        return new ResponseEntity<>(clienteService.consultarClientesActivos(), HttpStatus.OK);
    }

    // 9. Actualización completa de información de cliente (sin modificar CURP, RFC ni número de cuenta)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ClienteActualizacionRequest request) {
        return new ResponseEntity<>(clienteService.actualizarCliente(id, request), HttpStatus.OK);
    }

    // 10. Actualización parcial / Modificación con PATCH
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> patchCliente(
            @PathVariable("id") Integer id,
            @RequestBody ClienteActualizacionRequest request) {
        return new ResponseEntity<>(clienteService.actualizarCliente(id, request), HttpStatus.OK);
    }
}
