package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiTest {

    @Test
    @DisplayName("Debe instanciar y configurar la documentación OpenAPI correctamente")
    void testCustomOpenAPI() {
        OpenApi openApi = new OpenApi();
        OpenAPI result = openApi.customOpenAPI();

        assertNotNull(result);
        assertNotNull(result.getInfo());
        assertEquals("1.0.0", result.getInfo().getVersion());
        assertTrue(result.getInfo().getTitle().contains("API de Servicios Financieros"));
        assertNotNull(result.getComponents());
        assertTrue(result.getComponents().getSecuritySchemes().containsKey("BearerAuth"));
    }
}

