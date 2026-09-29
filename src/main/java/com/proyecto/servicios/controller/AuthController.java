package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.seguridad.UsuarioLogin;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository;
import com.proyecto.servicios.service.BiometriaMediaPipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioLoginRepository usuarioLoginRepository;
    private final ClienteRepository clienteRepository;
    private final BiometriaMediaPipeService biometriaMediaPipeService;

    public AuthController(UsuarioLoginRepository usuarioLoginRepository,
                          ClienteRepository clienteRepository,
                          BiometriaMediaPipeService biometriaMediaPipeService) {
        this.usuarioLoginRepository = usuarioLoginRepository;
        this.clienteRepository = clienteRepository;
        this.biometriaMediaPipeService = biometriaMediaPipeService;
    }

    // 1. Inicio de sesión con usuario y contraseña (configurado con expiración de 5 minutos / 300s por inactividad)
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UsuarioLogin usuario = usuarioLoginRepository.findByUsername(request.getUsername().toLowerCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Credenciales inválidas o usuario no encontrado"));

        if (!usuario.getActivo()) {
            throw new ReglaNegocioException("La cuenta de usuario se encuentra inactiva");
        }

        if (!usuario.getCliente().getActivo()) {
            throw new ReglaNegocioException("El cliente asociado se encuentra dado de baja (inactivo)");
        }

        if (!usuario.getPasswordHash().equals(request.getPassword())) {
            throw new ReglaNegocioException("Contraseña incorrecta");
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioLoginRepository.save(usuario);

        String sessionToken = "SESSION-JWT-" + UUID.randomUUID().toString().toUpperCase();

        LoginResponse response = LoginResponse.builder()
                .token(sessionToken)
                .tokenType("Bearer")
                .expiresInSeconds(300) // 5 minutos de inactividad obligatorios para cierre de app
                .mensajeInactividad("La sesión expirará automáticamente tras 5 minutos de inactividad")
                .clienteId(usuario.getCliente().getId())
                .nombreCliente(usuario.getCliente().getNombre() + " " + usuario.getCliente().getApellidoPaterno())
                .fechaAutenticacion(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 2. Registro de datos biométricos faciales cifrados (MediaPipe Face Embedding)
    @PostMapping(value = "/registrar-biometria", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registrarBiometria(@Valid @RequestBody BiometriaRequest request) {
        biometriaMediaPipeService.registrarBiometria(request);
        return new ResponseEntity<>("Datos biométricos faciales MediaPipe cifrados y guardados exitosamente", HttpStatus.CREATED);
    }

    // 3. Inicio de sesión mediante autenticación biométrica MediaPipe
    @PostMapping(value = "/login-biometrico", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> loginBiometrico(@Valid @RequestBody BiometriaRequest request) {
        if (request.getClienteId() == null) {
            throw new ReglaNegocioException("El ID de cliente es obligatorio para la autenticación biométrica");
        }

        boolean coincide = biometriaMediaPipeService.verificarBiometria(request.getClienteId(), request.getFaceEmbedding());
        if (!coincide) {
            throw new ReglaNegocioException("Autenticación biométrica MediaPipe fallida: rostro no coincide");
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));

        if (!cliente.getActivo()) {
            throw new ReglaNegocioException("El cliente se encuentra inactivo");
        }

        String sessionToken = "BIOMETRIC-SESSION-" + UUID.randomUUID().toString().toUpperCase();

        LoginResponse response = LoginResponse.builder()
                .token(sessionToken)
                .tokenType("Bearer")
                .expiresInSeconds(300) // 5 minutos de inactividad
                .mensajeInactividad("Autenticación biométrica exitosa. Sesión expira en 5 minutos por inactividad")
                .clienteId(cliente.getId())
                .nombreCliente(cliente.getNombre() + " " + cliente.getApellidoPaterno())
                .fechaAutenticacion(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
