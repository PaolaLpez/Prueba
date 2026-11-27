package com.proyecto.servicios.repositorys.producto;

import com.proyecto.servicios.entity.producto.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, String> {
    Optional<Producto> findByIdProducto(Integer idProducto);
}