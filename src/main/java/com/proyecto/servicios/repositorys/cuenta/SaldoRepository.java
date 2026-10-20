package com.proyecto.servicios.repositorys.cuenta;

import com.proyecto.servicios.entity.cuenta.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Integer> {
    Optional<Saldo> findByCuentaId(Integer cuentaId);
}
