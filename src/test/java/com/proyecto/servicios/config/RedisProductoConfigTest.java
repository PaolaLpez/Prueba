package com.proyecto.servicios.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RedisProductoConfigTest {

    @Test
    @DisplayName("Debe configurar el bean RedisTemplate con los serializadores de cadena")
    void testProductosRedisTemplate() {
        RedisProductoConfig config = new RedisProductoConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisTemplate<String, String> template = config.productosRedisTemplate(connectionFactory);

        assertNotNull(template);
        assertNotNull(template.getKeySerializer());
        assertNotNull(template.getValueSerializer());
    }
}

