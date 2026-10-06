package com.proyecto.servicios.model.catalogo;

import java.util.Arrays;
import java.util.List;

/**
 * Catálogo oficial de Nacionalidades permitidas en la aplicación.
 */
public enum Nacionalidad {
    MEXICANA("MEXICANA"),
    EXTRANJERA("EXTRANJERA"),
    ESTADOUNIDENSE("ESTADOUNIDENSE"),
    CANADIENSE("CANADIENSE"),
    ESPANIOLA("ESPANIOLA"),
    COLOMBIANA("COLOMBIANA"),
    ARGENTINA("ARGENTINA"),
    OTRA("OTRA");

    private final String valor;

    Nacionalidad(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static boolean esValida(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }
        String limpio = texto.trim().toUpperCase().replace("Ñ", "NI");
        return Arrays.stream(values())
                .anyMatch(n -> n.valor.equalsIgnoreCase(limpio) 
                            || n.valor.equalsIgnoreCase(texto.trim().toUpperCase()) 
                            || n.name().equalsIgnoreCase(limpio));
    }

    public static String normalizar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String upper = texto.trim().toUpperCase();
        if (upper.equals("ESPAÑOLA") || upper.equals("ESPANIOLA")) {
            return "ESPANIOLA";
        }
        for (Nacionalidad n : values()) {
            if (n.valor.equalsIgnoreCase(upper) || n.name().equalsIgnoreCase(upper)) {
                return n.valor;
            }
        }
        return upper;
    }

    public static List<String> valoresPermitidos() {
        return Arrays.stream(values()).map(Nacionalidad::getValor).toList();
    }
}
