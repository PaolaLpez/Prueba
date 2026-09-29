package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreo(String correo);

    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreo(String correo);

    List<Cliente> findByActivoTrue();
    List<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
}
