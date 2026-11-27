package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.service.PersonaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    @Mock
    private PersonaService personaService;

    @InjectMocks
    private PersonaController personaController;

    @Test
    @DisplayName("Debe crear usuario desde PersonaController")
    void testCrearUser() {
        PersonasRequest request = new PersonasRequest();
        PersonaResponse response = new PersonaResponse();
        when(personaService.creaPersona(request)).thenReturn(response);

        ResponseEntity<GenericResponse> result = personaController.crearUser(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(personaService, times(1)).creaPersona(request);
    }

    @Test
    @DisplayName("Debe actualizar usuario desde PersonaController")
    void testActualizUser() {
        PersonasRequest request = new PersonasRequest();
        GenericResponse response = new GenericResponse();
        when(personaService.actualizaPersona(request)).thenReturn(response);

        ResponseEntity<GenericResponse> result = personaController.actualizUser(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(personaService, times(1)).actualizaPersona(request);
    }

    @Test
    @DisplayName("Debe eliminar usuario desde PersonaController")
    void testEliminaUser() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        GenericResponse response = new GenericResponse();
        when(personaService.eliminaPersona(request)).thenReturn(response);

        ResponseEntity<GenericResponse> result = personaController.actualizUser(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(personaService, times(1)).eliminaPersona(request);
    }
}

