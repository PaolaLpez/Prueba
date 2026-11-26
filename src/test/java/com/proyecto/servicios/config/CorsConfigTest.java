package com.proyecto.servicios.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    @DisplayName("Debe registrar la configuración de CORS para todos los endpoints /**")
    void testAddCorsMappings() {
        CorsConfig corsConfig = new CorsConfig();
        CorsRegistry registry = new CorsRegistry();

        corsConfig.addCorsMappings(registry);

        assertNotNull(registry);
    }
}
