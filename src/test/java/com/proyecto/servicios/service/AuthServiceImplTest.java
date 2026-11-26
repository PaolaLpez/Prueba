package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.seguridad.UsuarioLogin;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioLoginRepository usuarioLoginRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private BiometriaMediaPipeService biometriaMediaPipeService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Cliente clienteTest;
    private UsuarioLogin usuarioTest;

    @BeforeEach
    void setUp() {
        clienteTest = new Cliente();
        clienteTest.setId(1);
        clienteTest.setNombre("Paola");
        clienteTest.setApellidoPaterno("Lopez");
        clienteTest.setActivo(true);

        usuarioTest = new UsuarioLogin();
        usuarioTest.setId(1);
        usuarioTest.setUsername("paola");
        usuarioTest.setPasswordHash("pass123");
        usuarioTest.setActivo(true);
        usuarioTest.setCliente(clienteTest);
    }

    @Test
    @DisplayName("Login exitoso con credenciales correctas")
    void testLogin_Exitoso() {
        LoginRequest request = new LoginRequest();
        request.setUsername("paola");
        request.setPassword("pass123");

        when(usuarioLoginRepository.findByUsername("paola")).thenReturn(Optional.of(usuarioTest));

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1, response.getClienteId());
        verify(usuarioLoginRepository, times(1)).save(usuarioTest);
    }

    @Test
    @DisplayName("Debe fallar login si el usuario no existe")
    void testLogin_UsuarioNoExiste() {
        LoginRequest request = new LoginRequest();
        request.setUsername("desconocido");
        request.setPassword("pass123");

        when(usuarioLoginRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe fallar login si la cuenta de usuario está inactiva")
    void testLogin_UsuarioInactivo() {
        usuarioTest.setActivo(false);
        LoginRequest request = new LoginRequest();
        request.setUsername("paola");
        request.setPassword("pass123");

        when(usuarioLoginRepository.findByUsername("paola")).thenReturn(Optional.of(usuarioTest));

        assertThrows(ReglaNegocioException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe fallar login si el cliente asociado está inactivo")
    void testLogin_ClienteInactivo() {
        clienteTest.setActivo(false);
        LoginRequest request = new LoginRequest();
        request.setUsername("paola");
        request.setPassword("pass123");

        when(usuarioLoginRepository.findByUsername("paola")).thenReturn(Optional.of(usuarioTest));

        assertThrows(ReglaNegocioException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe fallar login con contraseña incorrecta")
    void testLogin_PasswordIncorrecto() {
        LoginRequest request = new LoginRequest();
        request.setUsername("paola");
        request.setPassword("password_erroneo");

        when(usuarioLoginRepository.findByUsername("paola")).thenReturn(Optional.of(usuarioTest));

        assertThrows(ReglaNegocioException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe delegar registro biométrico a BiometriaMediaPipeService")
    void testRegistrarBiometria() {
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(1);

        authService.registrarBiometria(request);

        verify(biometriaMediaPipeService, times(1)).registrarBiometria(request);
    }

    @Test
    @DisplayName("Login biométrico exitoso")
    void testLoginBiometrico_Exitoso() {
        double[] vector = new double[]{0.1, 0.2, 0.3};
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(1);
        request.setFaceEmbedding(vector);

        when(biometriaMediaPipeService.verificarBiometria(1, vector)).thenReturn(true);
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));

        LoginResponse response = authService.loginBiometrico(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(1, response.getClienteId());
    }

    @Test
    @DisplayName("Debe fallar login biométrico si no envía clienteId")
    void testLoginBiometrico_SinClienteId() {
        BiometriaRequest request = new BiometriaRequest();
        assertThrows(ReglaNegocioException.class, () -> authService.loginBiometrico(request));
    }

    @Test
    @DisplayName("Debe fallar login biométrico si rostro no coincide")
    void testLoginBiometrico_RostroNoCoincide() {
        double[] vector = new double[]{0.1, 0.2};
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(1);
        request.setFaceEmbedding(vector);

        when(biometriaMediaPipeService.verificarBiometria(1, vector)).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> authService.loginBiometrico(request));
    }

    @Test
    @DisplayName("Debe fallar login biométrico si el cliente no existe en base de datos")
    void testLoginBiometrico_ClienteNoExiste() {
        double[] vector = new double[]{0.1, 0.2};
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(999);
        request.setFaceEmbedding(vector);

        when(biometriaMediaPipeService.verificarBiometria(999, vector)).thenReturn(true);
        when(clienteRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> authService.loginBiometrico(request));
    }

    @Test
    @DisplayName("Debe fallar login biométrico si el cliente se encuentra inactivo")
    void testLoginBiometrico_ClienteInactivo() {
        double[] vector = new double[]{0.1, 0.2};
        BiometriaRequest request = new BiometriaRequest();
        request.setClienteId(1);
        request.setFaceEmbedding(vector);

        clienteTest.setActivo(false);
        when(biometriaMediaPipeService.verificarBiometria(1, vector)).thenReturn(true);
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clienteTest));

        assertThrows(ReglaNegocioException.class, () -> authService.loginBiometrico(request));
    }
}
