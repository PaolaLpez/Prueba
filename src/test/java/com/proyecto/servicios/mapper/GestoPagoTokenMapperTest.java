package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoTokenMapperTest {

    private final GestoPagoTokenMapper mapper = Mappers.getMapper(GestoPagoTokenMapper.class);

    @Test
    @DisplayName("Debe mapear GestoPagoAuthResponse a GestoPagoToken y manejar nulos")
    void testToEntity() {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("TOKEN-123");
        response.setStatus(200);
        response.setMessage("OK");

        GestoPagoToken token = mapper.toEntity(response);

        assertNotNull(token);
        assertEquals("TOKEN-123", token.getToken());

        // Null check
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("Debe actualizar entidad existente con updateEntity y manejar nulos")
    void testUpdateEntity() {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("TOKEN-NUEVO");

        GestoPagoToken token = new GestoPagoToken();
        token.setId(1);
        token.setToken("TOKEN-VIEJO");

        mapper.updateEntity(response, token);

        assertEquals("TOKEN-NUEVO", token.getToken());
        assertEquals(1, token.getId());

        // Null check
        mapper.updateEntity(null, token);
    }
}
