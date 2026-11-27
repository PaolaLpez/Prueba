package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
 
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByNumeroCuenta(String numeroCuenta);
    List<Pago> findByClienteId(Integer clienteId);

    @Query("SELECT p FROM Pago p WHERE LOWER(p.cliente.correo) = LOWER(:correo) ORDER BY p.fechaCreacion DESC, p.id DESC")
    List<Pago> findByCorreoIgnoreCase(@Param("correo") String correo);

    @Query("SELECT p FROM Pago p WHERE p.cliente.id = :clienteId ORDER BY p.fechaCreacion DESC, p.id DESC")
    List<Pago> findByClienteIdOrderByFechaDesc(@Param("clienteId") Integer clienteId);

    @Query("SELECT p FROM Pago p WHERE p.numeroCuenta = :numeroCuenta ORDER BY p.fechaCreacion DESC, p.id DESC")
    List<Pago> findByNumeroCuentaOrderByFechaDesc(@Param("numeroCuenta") String numeroCuenta);

    Optional<Pago> findByTrace(String trace);
    Optional<Pago> findByUpc(String upc);
    Optional<Pago> findByReferencia(String referencia);
    boolean existsByReferenciaAndEstado(String referencia, String estado);
}
