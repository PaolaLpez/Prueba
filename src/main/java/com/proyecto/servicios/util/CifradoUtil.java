package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utilidad de cifrado AES y generación de valores requeridos para transacciones seguras (UPC, hashes, etc.).
 */
@Component
@Slf4j
public class CifradoUtil {

    private static final String DEFAULT_SECRET = "PuntoredGestoPagoSecureSecretKey2026!";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private final SecretKeySpec secretKeySpec;

    @Autowired
    public CifradoUtil(@Value("${cifrado.secret:PuntoredGestoPagoSecureSecretKey2026!}") String customSecret) {
        this.secretKeySpec = generarKeySpec(customSecret != null && !customSecret.isBlank() ? customSecret : DEFAULT_SECRET);
    }

    private SecretKeySpec generarKeySpec(String secret) {
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Usar clave AES de 128 bits compatible
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception ex) {
            log.error("Error al inicializar la clave de cifrado: {}", ex.getMessage());
            return new SecretKeySpec(Arrays.copyOf(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8), 16), ALGORITHM);
        }
    }

    /**
     * Cifra un texto plano o JSON utilizando AES y retorna la cadena en Base64.
     */
    public String cifrarAES(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] bytesCifrados = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytesCifrados);
        } catch (Exception ex) {
            log.error("Fallo al cifrar datos con AES: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Error al cifrar la petición de compra: " + ex.getMessage(), ex);
        }
    }

    /**
     * Descifra una cadena en Base64 utilizando AES y retorna el texto original.
     */
    public String descifrarAES(String textoCifradoBase64) {
        if (textoCifradoBase64 == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] bytesOriginales = cipher.doFinal(Base64.getDecoder().decode(textoCifradoBase64));
            return new String(bytesOriginales, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Fallo al descifrar datos con AES: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Error al descifrar la petición: " + ex.getMessage(), ex);
        }
    }

    /**
     * Genera un UPC aleatorio numérico de exactamente 10 dígitos:
     * "upc (random string de 10 digitos)"
     */
    public static String generarUpcAleatorio() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(0, 10));
        }
        return sb.toString();
    }
}
