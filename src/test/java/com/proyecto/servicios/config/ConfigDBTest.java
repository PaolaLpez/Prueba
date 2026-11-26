package com.proyecto.servicios.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigDBTest {

    @Test
    @DisplayName("Debe instanciar ConfigDB correctamente")
    void testConfigDB() {
        ConfigDB config = new ConfigDB();
        assertNotNull(config);
    }
}

