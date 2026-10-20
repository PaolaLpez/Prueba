package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer>, JpaSpecificationExecutor<Cliente> {

    @Override
    @EntityGraph(attributePaths = {"domicilios"})
    List<Cliente> findAll();

    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreo(String correo);

    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreo(String correo);

    boolean existsByCurpIgnoreCase(String curp);
    boolean existsByRfcIgnoreCase(String rfc);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByTelefonoMovil(String telefonoMovil);

    boolean existsByNombreIgnoreCaseAndApellidoPaternoIgnoreCaseAndApellidoMaternoIgnoreCase(
            String nombre, String apellidoPaterno, String apellidoMaterno);

    boolean existsByNombreIgnoreCaseAndApellidoPaternoIgnoreCaseAndApellidoMaternoIgnoreCaseAndFechaNacimiento(
            String nombre, String apellidoPaterno, String apellidoMaterno, LocalDate fechaNacimiento);

    List<Cliente> findByActivoTrue();
    List<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
}
