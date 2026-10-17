package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.seguridad.UsuarioLogin;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.BiometriaMediaPipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UsuarioLoginRepository usuarioLoginRepository;
    private final ClienteRepository clienteRepository;
    private final BiometriaMediaPipeService biometriaMediaPipeService;

    public AuthServiceImpl(UsuarioLoginRepository usuarioLoginRepository,
                           ClienteRepository clienteRepository,
                           BiometriaMediaPipeService biometriaMediaPipeService) {
        this.usuarioLoginRepository = usuarioLoginRepository;
        this.clienteRepository = clienteRepository;
        this.biometriaMediaPipeService = biometriaMediaPipeService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
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

        return LoginResponse.builder()
                .token(sessionToken)
                .tokenType("Bearer")
                .expiresInSeconds(300)
                .mensajeInactividad("La sesión expirará automáticamente tras 5 minutos de inactividad")
                .clienteId(usuario.getCliente().getId())
                .nombreCliente(usuario.getCliente().getNombre() + " " + usuario.getCliente().getApellidoPaterno())
                .fechaAutenticacion(LocalDateTime.now())
                .build();
    }

    @Override
    public void registrarBiometria(BiometriaRequest request) {
        biometriaMediaPipeService.registrarBiometria(request);
    }

    @Override
    public LoginResponse loginBiometrico(BiometriaRequest request) {
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

        return LoginResponse.builder()
                .token(sessionToken)
                .tokenType("Bearer")
                .expiresInSeconds(300)
                .mensajeInactividad("Autenticación biométrica exitosa. Sesión expira en 5 minutos por inactividad")
                .clienteId(cliente.getId())
                .nombreCliente(cliente.getNombre() + " " + cliente.getApellidoPaterno())
                .fechaAutenticacion(LocalDateTime.now())
                .build();
    }
}
