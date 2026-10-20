package com.proyecto.servicios.repositorys.producto;

import com.proyecto.servicios.entity.producto.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, String> {
}