package com.proyecto.servicios.entity.seguridad;

import com.proyecto.servicios.entity.cliente.Cliente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "datos_biometricos")
@Getter
@Setter
public class DatosBiometricos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo_biometrico", nullable = false, length = 50)
    private String tipoBiometrico = "MEDIAPIPE_FACE_EMBEDDING";

    @Column(name = "face_embedding_cifrado", nullable = false, columnDefinition = "TEXT")
    private String faceEmbeddingCifrado;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
