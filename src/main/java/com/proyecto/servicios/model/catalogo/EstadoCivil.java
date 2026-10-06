package com.proyecto.servicios.model.catalogo;

import java.util.Arrays;
import java.util.List;

/**
 * Catálogo oficial de Estado Civil permitido en la aplicación:
 * SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION LIBRE
 */
public enum EstadoCivil {
    SOLTERO("SOLTERO"),
    CASADO("CASADO"),
    DIVORCIADO("DIVORCIADO"),
    VIUDO("VIUDO"),
    UNION_LIBRE("UNION LIBRE");

    private final String valor;

    EstadoCivil(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static boolean esValido(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }
        String limpio = texto.trim().toUpperCase().replace("_", " ");
        return Arrays.stream(values())
                .anyMatch(ec -> ec.valor.equalsIgnoreCase(limpio) || ec.name().equalsIgnoreCase(limpio));
    }

    public static String normalizar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.trim().toUpperCase().replace("_", " ");
        for (EstadoCivil ec : values()) {
            if (ec.valor.equalsIgnoreCase(limpio) || ec.name().equalsIgnoreCase(limpio)) {
                return ec.valor;
            }
        }
        return limpio;
    }

    public static List<String> valoresPermitidos() {
        return Arrays.stream(values()).map(EstadoCivil::getValor).toList();
    }
}
