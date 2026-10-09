package com.proyecto.servicios.repositorys.seguridad;

import com.proyecto.servicios.entity.seguridad.DatosBiometricos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatosBiometricosRepository extends JpaRepository<DatosBiometricos, Integer> {
    Optional<DatosBiometricos> findByClienteId(Integer clienteId);
}
