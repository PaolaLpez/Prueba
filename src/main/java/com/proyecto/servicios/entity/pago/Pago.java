package com.proyecto.servicios.entity.pago;

import com.proyecto.servicios.entity.cliente.Cliente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor; 
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    private String numeroCuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "producto_id", nullable = false, length = 50)
    private String productoId;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "telefono", nullable = false, length = 15)
    private String telefono;

    @Column(name = "referencia", nullable = false, length = 100)
    private String referencia;

    @Column(name = "hora_local", nullable = false, length = 30)
    private String horaLocal;

    @Column(name = "upc", nullable = false, length = 20)
    private String upc;

    @Column(name = "unidad", nullable = false, length = 20)
    private String unidad;

    @Column(name = "peticion_cifrada", columnDefinition = "TEXT")
    private String peticionCifrada;

    @Column(name = "token", columnDefinition = "TEXT")
    private String token;

    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "EXITOSO";

    @Column(name = "trace", length = 50)
    private String trace;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "EXITOSO";
        }
    }
}
