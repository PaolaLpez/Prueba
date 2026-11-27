package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.Impl.PersonasServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonasServiceImplTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonasServiceImpl personasService;

    @Test
    @DisplayName("Debe crear persona exitosamente")
    void testCreaPersona() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Juan");
        request.setApellidoP("Perez");
        request.setApellidoMaterno("Lopez");

        PersonaResponse response = personasService.creaPersona(request);

        assertNotNull(response);
        assertEquals(1, response.getCodigo());
        assertEquals("Exito", response.getMensaje());
        verify(personasRepository, times(1)).save(any(Personas.class));
    }

    @Test
    @DisplayName("Debe eliminar persona cuando existe")
    void testEliminaPersona_Existe() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Juan");

        Personas p = new Personas();
        p.setNombre("Juan");
        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(p));

        GenericResponse response = personasService.eliminaPersona(request);

        assertEquals(0, response.getCodigo());
        verify(personasRepository, times(1)).delete(p);
    }

    @Test
    @DisplayName("Debe fallar al eliminar persona cuando no existe")
    void testEliminaPersona_NoExiste() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Desconocido");

        when(personasRepository.findByNombre("Desconocido")).thenReturn(Optional.empty());

        GenericResponse response = personasService.eliminaPersona(request);

        assertEquals(1, response.getCodigo());
        verify(personasRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe actualizar persona cuando existe")
    void testActualizaPersona_Existe() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Juan");
        request.setApellidoP("NuevoApellido");

        Personas p = new Personas();
        p.setNombre("Juan");
        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(p));

        GenericResponse response = personasService.actualizaPersona(request);

        assertEquals(0, response.getCodigo());
        verify(personasRepository, times(1)).save(p);
    }

    @Test
    @DisplayName("Debe fallar al actualizar persona cuando no existe")
    void testActualizaPersona_NoExiste() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Desconocido");

        when(personasRepository.findByNombre("Desconocido")).thenReturn(Optional.empty());

        GenericResponse response = personasService.actualizaPersona(request);

        assertEquals(1, response.getCodigo());
    }
}

