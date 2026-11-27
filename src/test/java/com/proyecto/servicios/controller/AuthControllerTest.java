package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginResponse;
import com.proyecto.servicios.service.AuthService;
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
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Debe procesar login con credenciales")
    void testLogin() {
        LoginRequest request = new LoginRequest();
        LoginResponse response = LoginResponse.builder().token("TOKEN-123").build();
        when(authService.login(request)).thenReturn(response);

        ResponseEntity<LoginResponse> result = authController.login(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("TOKEN-123", result.getBody().getToken());
    }

    @Test
    @DisplayName("Debe registrar datos biométricos")
    void testRegistrarBiometria() {
        BiometriaRequest request = new BiometriaRequest();

        ResponseEntity<String> result = authController.registrarBiometria(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(authService, times(1)).registrarBiometria(request);
    }

    @Test
    @DisplayName("Debe procesar login biométrico")
    void testLoginBiometrico() {
        BiometriaRequest request = new BiometriaRequest();
        LoginResponse response = LoginResponse.builder().token("BIO-TOKEN-456").build();
        when(authService.loginBiometrico(request)).thenReturn(response);

        ResponseEntity<LoginResponse> result = authController.loginBiometrico(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("BIO-TOKEN-456", result.getBody().getToken());
    }
}

