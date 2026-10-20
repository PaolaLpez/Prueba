package com.proyecto.servicios.service;

import com.proyecto.servicios.model.dto.seguridad.BiometriaRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginRequest;
import com.proyecto.servicios.model.dto.seguridad.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void registrarBiometria(BiometriaRequest request);
    LoginResponse loginBiometrico(BiometriaRequest request);
}
