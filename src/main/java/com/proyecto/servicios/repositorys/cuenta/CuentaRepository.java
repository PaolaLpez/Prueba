package com.proyecto.servicios.repositorys.cuenta;

import com.proyecto.servicios.entity.cuenta.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    boolean existsByNumeroCuenta(String numeroCuenta);
    List<Cuenta> findByEstatus(String estatus);
    List<Cuenta> findByClienteId(Integer clienteId);
}
