package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
 
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByNumeroCuenta(String numeroCuenta);
    List<Pago> findByClienteId(Integer clienteId);
    Optional<Pago> findByTrace(String trace);
    Optional<Pago> findByUpc(String upc);
    Optional<Pago> findByReferencia(String referencia);
    boolean existsByReferenciaAndEstado(String referencia, String estado);
}
