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
import java.util.Optional;

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
            if (productoRepository.count() < 900) {
                log.info("Catálogo en base de datos insuficiente (< 900 productos). Cargando catálogo completo oficial de 910 productos...");
                productoService.cargarCatalogoSemilla();
            } else {
                log.info("Catálogo de productos verificado: {} productos disponibles en base de datos.", productoRepository.count());
            }
        } catch (Exception ex) {
            log.warn("No fue posible inicializar o sembrar catálogo de productos al iniciar: {}", ex.getMessage());
        }

        try {
            com.proyecto.servicios.repositorys.cuenta.CuentaRepository cuentaRepo = context.getBean(com.proyecto.servicios.repositorys.cuenta.CuentaRepository.class);
            com.proyecto.servicios.repositorys.cliente.ClienteRepository clienteRepo = context.getBean(com.proyecto.servicios.repositorys.cliente.ClienteRepository.class);
            com.proyecto.servicios.repositorys.cuenta.SaldoRepository saldoRepo = context.getBean(com.proyecto.servicios.repositorys.cuenta.SaldoRepository.class);

            // Asegurar que exista el cliente Paola López con correo lopezpao869@gmail.com y cuenta 5730094736
            com.proyecto.servicios.entity.cliente.Cliente cli = clienteRepo.findByCorreo("lopezpao869@gmail.com")
                    .or(() -> clienteRepo.findById(3))
                    .orElseGet(() -> {
                        com.proyecto.servicios.entity.cliente.Cliente nuevo = new com.proyecto.servicios.entity.cliente.Cliente();
                        nuevo.setNombre("Paola");
                        nuevo.setApellidoPaterno("Lopez");
                        nuevo.setApellidoMaterno("Garcia");
                        nuevo.setCurp("LOGP980515MMCPRL09");
                        nuevo.setRfc("LOGP980515AA0");
                        nuevo.setCorreo("lopezpao869@gmail.com");
                        nuevo.setTelefonoMovil("5512345678");
                        nuevo.setFechaNacimiento(java.time.LocalDate.of(1998, 5, 15));
                        nuevo.setIngresoMensual(new BigDecimal("30000.00"));
                        nuevo.setActivo(true);
                        return clienteRepo.save(nuevo);
                    });

            boolean modificado = false;
            if (!"lopezpao869@gmail.com".equalsIgnoreCase(cli.getCorreo())) {
                cli.setCorreo("lopezpao869@gmail.com");
                modificado = true;
            }
            if (!"5512345678".equals(cli.getTelefonoMovil())) {
                cli.setTelefonoMovil("5512345678");
                modificado = true;
            }
            if (cli.getActivo() == null || !cli.getActivo()) {
                cli.setActivo(true);
                modificado = true;
            }
            if (modificado) {
                cli = clienteRepo.save(cli);
            }

            Optional<com.proyecto.servicios.entity.cuenta.Cuenta> ctaOpt = cuentaRepo.findByNumeroCuenta("5730094736");
            com.proyecto.servicios.entity.cuenta.Cuenta cta;
            if (ctaOpt.isEmpty()) {
                cta = new com.proyecto.servicios.entity.cuenta.Cuenta();
                cta.setCliente(cli);
                cta.setNumeroCuenta("5730094736");
                cta.setEstatus("ACTIVA");
                cta = cuentaRepo.save(cta);

                com.proyecto.servicios.entity.cuenta.Saldo sld = new com.proyecto.servicios.entity.cuenta.Saldo();
                sld.setCuenta(cta);
                sld.setSaldoDisponible(new BigDecimal("1500.00"));
                sld.setSaldoRetenido(BigDecimal.ZERO);
                saldoRepo.save(sld);
                log.info("Cliente Paola López (ID {}), Cuenta 5730094736 y Saldo ($1,500.00) creados exitosamente.", cli.getId());
            } else {
                cta = ctaOpt.get();
                if (cta.getCliente() == null || !cli.getId().equals(cta.getCliente().getId()) || !"ACTIVA".equalsIgnoreCase(cta.getEstatus())) {
                    cta.setCliente(cli);
                    cta.setEstatus("ACTIVA");
                    cuentaRepo.save(cta);
                }
                Optional<com.proyecto.servicios.entity.cuenta.Saldo> sldOpt = saldoRepo.findByCuentaId(cta.getId());
                if (sldOpt.isEmpty()) {
                    com.proyecto.servicios.entity.cuenta.Saldo sld = new com.proyecto.servicios.entity.cuenta.Saldo();
                    sld.setCuenta(cta);
                    sld.setSaldoDisponible(new BigDecimal("1500.00"));
                    sld.setSaldoRetenido(BigDecimal.ZERO);
                    saldoRepo.save(sld);
                } else {
                    com.proyecto.servicios.entity.cuenta.Saldo sld = sldOpt.get();
                    if (sld.getSaldoDisponible() == null || sld.getSaldoDisponible().compareTo(new BigDecimal("250.00")) < 0) {
                        sld.setSaldoDisponible(new BigDecimal("1500.00"));
                        saldoRepo.save(sld);
                    }
                }
                log.info("Cliente Paola López (ID {}), Cuenta 5730094736 vinculada y activa.", cli.getId());
            }

            // Asegurar credenciales de acceso para lopezpao869@gmail.com con Password123!
            try {
                final com.proyecto.servicios.entity.cliente.Cliente clienteFinal = cli;
                final Integer clienteId = cli.getId();
                com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository loginRepo = context.getBean(com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository.class);
                com.proyecto.servicios.entity.seguridad.UsuarioLogin userLogin = loginRepo.findByUsername("lopezpao869@gmail.com")
                        .or(() -> loginRepo.findByClienteId(clienteId))
                        .orElseGet(() -> {
                            com.proyecto.servicios.entity.seguridad.UsuarioLogin ul = new com.proyecto.servicios.entity.seguridad.UsuarioLogin();
                            ul.setCliente(clienteFinal);
                            ul.setUsername("lopezpao869@gmail.com");
                            return ul;
                        });
                userLogin.setCliente(clienteFinal);
                userLogin.setUsername("lopezpao869@gmail.com");
                userLogin.setPasswordHash("Password123!");
                userLogin.setRol("CLIENTE");
                userLogin.setActivo(true);
                loginRepo.save(userLogin);
                log.info("Credenciales de login para lopezpao869@gmail.com aseguradas con contraseña: Password123!");
            } catch (Exception ex) {
                log.warn("No fue posible inicializar credenciales de login: {}", ex.getMessage());
            }

            // Asegurar usuario y cuenta de ADMINISTRADOR (admin / Password123!)
            try {
                com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository loginRepo = context.getBean(com.proyecto.servicios.repositorys.seguridad.UsuarioLoginRepository.class);
                com.proyecto.servicios.entity.cliente.Cliente adminCli = clienteRepo.findByCorreo("admin@gestopago.com")
                        .orElseGet(() -> {
                            com.proyecto.servicios.entity.cliente.Cliente nuevoAdmin = new com.proyecto.servicios.entity.cliente.Cliente();
                            nuevoAdmin.setNombre("Administrador");
                            nuevoAdmin.setApellidoPaterno("General");
                            nuevoAdmin.setApellidoMaterno("GestoPago");
                            nuevoAdmin.setCurp("ADMI900101HDFRRN01");
                            nuevoAdmin.setRfc("ADMI900101AA1");
                            nuevoAdmin.setCorreo("admin@gestopago.com");
                            nuevoAdmin.setTelefonoMovil("5500000000");
                            nuevoAdmin.setFechaNacimiento(java.time.LocalDate.of(1990, 1, 1));
                            nuevoAdmin.setIngresoMensual(new BigDecimal("100000.00"));
                            nuevoAdmin.setActivo(true);
                            nuevoAdmin.setSexo("MASCULINO");
                            nuevoAdmin.setNacionalidad("MEXICANA");
                            nuevoAdmin.setEstadoCivil("SOLTERO");
                            return clienteRepo.save(nuevoAdmin);
                        });

                Optional<com.proyecto.servicios.entity.cuenta.Cuenta> adminCtaOpt = cuentaRepo.findByNumeroCuenta("9999000001");
                com.proyecto.servicios.entity.cuenta.Cuenta adminCta;
                if (adminCtaOpt.isEmpty()) {
                    adminCta = new com.proyecto.servicios.entity.cuenta.Cuenta();
                    adminCta.setCliente(adminCli);
                    adminCta.setNumeroCuenta("9999000001");
                    adminCta.setEstatus("ACTIVA");
                    adminCta = cuentaRepo.save(adminCta);

                    com.proyecto.servicios.entity.cuenta.Saldo adminSld = new com.proyecto.servicios.entity.cuenta.Saldo();
                    adminSld.setCuenta(adminCta);
                    adminSld.setSaldoDisponible(new BigDecimal("100000.00"));
                    adminSld.setSaldoRetenido(BigDecimal.ZERO);
                    saldoRepo.save(adminSld);
                } else {
                    adminCta = adminCtaOpt.get();
                    Optional<com.proyecto.servicios.entity.cuenta.Saldo> sldOpt = saldoRepo.findByCuentaId(adminCta.getId());
                    if (sldOpt.isEmpty()) {
                        com.proyecto.servicios.entity.cuenta.Saldo adminSld = new com.proyecto.servicios.entity.cuenta.Saldo();
                        adminSld.setCuenta(adminCta);
                        adminSld.setSaldoDisponible(new BigDecimal("100000.00"));
                        adminSld.setSaldoRetenido(BigDecimal.ZERO);
                        saldoRepo.save(adminSld);
                    } else if (sldOpt.get().getSaldoDisponible().compareTo(new BigDecimal("5000.00")) < 0) {
                        com.proyecto.servicios.entity.cuenta.Saldo adminSld = sldOpt.get();
                        adminSld.setSaldoDisponible(new BigDecimal("100000.00"));
                        saldoRepo.save(adminSld);
                    }
                }

                // Crear o actualizar login admin
                final com.proyecto.servicios.entity.cliente.Cliente finalAdmin = adminCli;
                com.proyecto.servicios.entity.seguridad.UsuarioLogin adminLogin = loginRepo.findByUsername("admin")
                        .or(() -> loginRepo.findByUsername("admin@gestopago.com"))
                        .orElseGet(() -> {
                            com.proyecto.servicios.entity.seguridad.UsuarioLogin ul = new com.proyecto.servicios.entity.seguridad.UsuarioLogin();
                            ul.setCliente(finalAdmin);
                            ul.setUsername("admin");
                            return ul;
                        });
                adminLogin.setCliente(finalAdmin);
                adminLogin.setUsername("admin");
                adminLogin.setPasswordHash("Password123!");
                adminLogin.setRol("ADMIN");
                adminLogin.setActivo(true);
                loginRepo.save(adminLogin);
                log.info("Usuario Administrador ('admin' / 'Password123!') con cuenta 9999000001 listo y activo.");

                // Asegurar Administrador Maria Jackeline Mata con correo mariajacke.mata@gmail.com
                com.proyecto.servicios.entity.cliente.Cliente mariaCli = clienteRepo.findByCorreo("mariajacke.mata@gmail.com")
                        .orElseGet(() -> {
                            com.proyecto.servicios.entity.cliente.Cliente nuevo = new com.proyecto.servicios.entity.cliente.Cliente();
                            nuevo.setNombre("Maria");
                            nuevo.setApellidoPaterno("Jackeline");
                            nuevo.setApellidoMaterno("Mata");
                            nuevo.setCurp("MATM980101MDFRRN01");
                            nuevo.setRfc("MATM980101AA1");
                            nuevo.setCorreo("mariajacke.mata@gmail.com");
                            nuevo.setTelefonoMovil("5512345679");
                            nuevo.setFechaNacimiento(java.time.LocalDate.of(1998, 1, 1));
                            nuevo.setIngresoMensual(new BigDecimal("100000.00"));
                            nuevo.setActivo(true);
                            nuevo.setSexo("FEMENINO");
                            nuevo.setNacionalidad("MEXICANA");
                            nuevo.setEstadoCivil("SOLTERO");
                            return clienteRepo.save(nuevo);
                        });

                Optional<com.proyecto.servicios.entity.cuenta.Cuenta> mariaCtaOpt = cuentaRepo.findByNumeroCuenta("9999000002");
                com.proyecto.servicios.entity.cuenta.Cuenta mariaCta;
                if (mariaCtaOpt.isEmpty()) {
                    mariaCta = new com.proyecto.servicios.entity.cuenta.Cuenta();
                    mariaCta.setCliente(mariaCli);
                    mariaCta.setNumeroCuenta("9999000002");
                    mariaCta.setEstatus("ACTIVA");
                    mariaCta = cuentaRepo.save(mariaCta);

                    com.proyecto.servicios.entity.cuenta.Saldo mariaSld = new com.proyecto.servicios.entity.cuenta.Saldo();
                    mariaSld.setCuenta(mariaCta);
                    mariaSld.setSaldoDisponible(new BigDecimal("100000.00"));
                    mariaSld.setSaldoRetenido(BigDecimal.ZERO);
                    saldoRepo.save(mariaSld);
                } else {
                    mariaCta = mariaCtaOpt.get();
                    Optional<com.proyecto.servicios.entity.cuenta.Saldo> sldOpt = saldoRepo.findByCuentaId(mariaCta.getId());
                    if (sldOpt.isEmpty()) {
                        com.proyecto.servicios.entity.cuenta.Saldo mariaSld = new com.proyecto.servicios.entity.cuenta.Saldo();
                        mariaSld.setCuenta(mariaCta);
                        mariaSld.setSaldoDisponible(new BigDecimal("100000.00"));
                        mariaSld.setSaldoRetenido(BigDecimal.ZERO);
                        saldoRepo.save(mariaSld);
                    } else if (sldOpt.get().getSaldoDisponible().compareTo(new BigDecimal("5000.00")) < 0) {
                        com.proyecto.servicios.entity.cuenta.Saldo mariaSld = sldOpt.get();
                        mariaSld.setSaldoDisponible(new BigDecimal("100000.00"));
                        saldoRepo.save(mariaSld);
                    }
                }

                final com.proyecto.servicios.entity.cliente.Cliente finalMaria = mariaCli;
                com.proyecto.servicios.entity.seguridad.UsuarioLogin mariaLogin = loginRepo.findByUsername("mariajacke.mata@gmail.com")
                        .orElseGet(() -> {
                            com.proyecto.servicios.entity.seguridad.UsuarioLogin ul = new com.proyecto.servicios.entity.seguridad.UsuarioLogin();
                            ul.setCliente(finalMaria);
                            ul.setUsername("mariajacke.mata@gmail.com");
                            return ul;
                        });
                mariaLogin.setCliente(finalMaria);
                mariaLogin.setUsername("mariajacke.mata@gmail.com");
                mariaLogin.setPasswordHash("Password123!");
                mariaLogin.setRol("ADMIN");
                mariaLogin.setActivo(true);
                loginRepo.save(mariaLogin);
                log.info("Usuario Administrador ('mariajacke.mata@gmail.com' / 'Password123!') con cuenta 9999000002 listo y activo.");
            } catch (Exception ex) {
                log.warn("No fue posible inicializar usuario Administrador: {}", ex.getMessage());
            }
        } catch (Exception ex) {
            log.warn("No fue posible inicializar o sincronizar la cuenta de prueba 5730094736: {}", ex.getMessage());
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
