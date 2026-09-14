package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.producto.ProductoResponse;

// Interfaz del servicio de negocio para la gestión de productos
public interface ProductoService {

    // Obtiene el listado de productos desde el servicio externo
    ProductoResponse obtenerProductos();
}
