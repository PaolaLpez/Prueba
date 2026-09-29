package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.dto.cliente.ClienteResponse;

import java.time.LocalDate;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRegistroRequest request);
    List<ClienteResponse> consultarTodosClientes();
    ClienteResponse consultarPorId(Integer id);
    ClienteResponse consultarPorCurp(String curp);
    ClienteResponse consultarPorRfc(String rfc);
    ClienteResponse consultarPorCorreo(String correo);
    ClienteResponse consultarPorNumeroCuenta(String numeroCuenta);
    List<ClienteResponse> consultarClientesActivos();
    List<ClienteResponse> consultarClientesPorRangoFechas(LocalDate inicio, LocalDate fin);
    ClienteResponse actualizarCliente(Integer id, ClienteActualizacionRequest request);
    void bajaLogicaCliente(Integer id);
}
