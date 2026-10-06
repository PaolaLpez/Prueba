package com.proyecto.servicios.repositorys.seguridad;

import com.proyecto.servicios.entity.seguridad.UsuarioLogin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioLoginRepository extends JpaRepository<UsuarioLogin, Integer> {

    @EntityGraph(attributePaths = {"cliente"})
    Optional<UsuarioLogin> findByUsername(String username);

    @EntityGraph(attributePaths = {"cliente"})
    Optional<UsuarioLogin> findByClienteId(Integer clienteId);

    boolean existsByUsername(String username);
}
