package com.proyecto.servicios;

import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@SpringBootApplication(scanBasePackages = {"com.proyecto.servicios", "com.proyecto.servicio.empresa"})
@EnableCaching
@EnableScheduling
@EnableFeignClients
@Slf4j
public class App implements CommandLineRunner {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoService productoService;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Sincronizando catálogo de productos desde GestoPago...");
            productoService.obtenerProductos();
        } catch (Exception ex) {
            log.warn("No fue posible sincronizar productos al iniciar: {}", ex.getMessage());
        }

        try {
            if (!productoRepository.existsById("1")) {
                Producto prod1 = new Producto();
                prod1.setId("1");
                prod1.setIdProducto(1);
                prod1.setIdServicio(1);
                prod1.setNombre("Recarga Telefónica y Pagos de Servicios");
                prod1.setPrecio(new BigDecimal("250.00"));
                prod1.setCategoria("Recargas y Servicios");
                prod1.setDisponible(true);
                productoRepository.save(prod1);
                log.info("Producto estándar de pruebas ID 1 ($250.00) registrado exitosamente.");
            }
        } catch (Exception ex) {
            log.warn("No fue posible registrar el producto ID 1: {}", ex.getMessage());
        }

        try {
            com.proyecto.servicios.repositorys.cuenta.CuentaRepository cuentaRepo = context.getBean(com.proyecto.servicios.repositorys.cuenta.CuentaRepository.class);
            com.proyecto.servicios.repositorys.cliente.ClienteRepository clienteRepo = context.getBean(com.proyecto.servicios.repositorys.cliente.ClienteRepository.class);
            com.proyecto.servicios.repositorys.cuenta.SaldoRepository saldoRepo = context.getBean(com.proyecto.servicios.repositorys.cuenta.SaldoRepository.class);

            if (cuentaRepo.findByNumeroCuenta("5730094736").isEmpty()) {
                com.proyecto.servicios.entity.cliente.Cliente cli = clienteRepo.findByCorreo("isapaola188@gmail.com")
                        .orElseGet(() -> {
                            com.proyecto.servicios.entity.cliente.Cliente nuevo = new com.proyecto.servicios.entity.cliente.Cliente();
                            nuevo.setNombre("Paola");
                            nuevo.setApellidoPaterno("Lopez");
                            nuevo.setApellidoMaterno("Garcia");
                            nuevo.setCurp("LOGP950520MDFR01");
                            nuevo.setRfc("LOGP9505201A0");
                            nuevo.setCorreo("isapaola188@gmail.com");
                            nuevo.setTelefonoMovil("5512345678");
                            nuevo.setFechaNacimiento(java.time.LocalDate.of(1995, 5, 20));
                            nuevo.setIngresoMensual(new BigDecimal("25000.00"));
                            nuevo.setActivo(true);
                            return clienteRepo.save(nuevo);
                        });

                com.proyecto.servicios.entity.cuenta.Cuenta cta = new com.proyecto.servicios.entity.cuenta.Cuenta();
                cta.setCliente(cli);
                cta.setNumeroCuenta("5730094736");
                cta.setEstatus("ACTIVA");
                cta = cuentaRepo.save(cta);

                com.proyecto.servicios.entity.cuenta.Saldo sld = new com.proyecto.servicios.entity.cuenta.Saldo();
                sld.setCuenta(cta);
                sld.setSaldoDisponible(new BigDecimal("2500.00"));
                sld.setSaldoRetenido(BigDecimal.ZERO);
                saldoRepo.save(sld);

                log.info("Cliente Paola López (ID {}), Cuenta 5730094736 y Saldo ($2,500.00) creados exitosamente para pruebas.", cli.getId());
            }
        } catch (Exception ex) {
            log.warn("No fue posible inicializar la cuenta de prueba 5730094736: {}", ex.getMessage());
        }
    }

    private static void displayInfo(BuildProperties buildProperties) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        String out = formatter.format(buildProperties.getTime());
        log.info("Nombre artefacto: " + buildProperties.getName() + "\n"
                + "Versión: " + buildProperties.getVersion() + "\n"
                + "Fecha Compilación: " + out + "\n"
                + "Artefacto: " + buildProperties.getArtifact() + "\n"
                + "Grupo: " + buildProperties.getGroup());
    }
}
