package com.proyecto.servicios.repositorys.cuenta;

import com.proyecto.servicios.entity.cuenta.Saldo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Integer> {

    Optional<Saldo> findByCuentaId(Integer cuentaId);

    // Bloqueo pesimista exclusivo para evitar condiciones de carrera en operaciones concurrentes
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Saldo s WHERE s.cuenta.id = :cuentaId")
    Optional<Saldo> findByCuentaIdForUpdate(@Param("cuentaId") Integer cuentaId);
}
