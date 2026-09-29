package com.proyecto.servicios.entity.cuenta;

import com.proyecto.servicios.entity.cliente.Cliente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 20)
    private String numeroCuenta;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus = "ACTIVA";

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDateTime fechaApertura;

    @OneToOne(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Saldo saldo;

    @PrePersist
    void onCreate() {
        if (fechaApertura == null) {
            fechaApertura = LocalDateTime.now();
        }
        if (estatus == null) {
            estatus = "ACTIVA";
        }
    }

    public void setSaldo(Saldo saldo) {
        this.saldo = saldo;
        if (saldo != null) {
            saldo.setCuenta(this);
        }
    }
}
