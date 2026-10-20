package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginResponse;
import com.proyecto.servicios.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 1. Inicio de sesión con usuario y contraseña (configurado con expiración de 5 minutos / 300s por inactividad)
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseEntity<>(authService.login(request), HttpStatus.OK);
    }

    // 2. Registro de datos biométricos faciales cifrados (MediaPipe Face Embedding)
    @PostMapping(value = "/registrar-biometria", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registrarBiometria(@Valid @RequestBody BiometriaRequest request) {
        authService.registrarBiometria(request);
        return new ResponseEntity<>("Datos biométricos faciales MediaPipe cifrados y guardados exitosamente", HttpStatus.CREATED);
    }

    // 3. Inicio de sesión mediante autenticación biométrica MediaPipe
    @PostMapping(value = "/login-biometrico", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> loginBiometrico(@Valid @RequestBody BiometriaRequest request) {
        return new ResponseEntity<>(authService.loginBiometrico(request), HttpStatus.OK);
    }
}
