package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.producto.ProductoDto;
import com.proyecto.servicios.model.dto.producto.ProductoResponse;
import java.util.List;

// Interfaz del servicio de negocio para la gestión de productos
public interface ProductoService {

    // Obtiene el listado de productos desde el servicio externo o base de datos
    ProductoResponse obtenerProductos();

    // Sincroniza forzadamente el catálogo completo consumiendo la API externa de GestoPago
    ProductoResponse sincronizarCatalogo();

    // Siembra el catálogo completo de 910 productos desde el recurso json empaquetado
    List<ProductoDto> cargarCatalogoSemilla();
}
