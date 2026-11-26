package com.proyecto.servicios.entity.cliente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ClienteEntityTest {

    @Test
    @DisplayName("Debe verificar campos, domicilios y ciclo de vida de Cliente y Domicilio")
    public void testClienteYDomicilio() {
        Cliente cli = new Cliente();
        cli.setId(1);
        cli.setNombre("Paola");
        cli.setSegundoNombre("Sofia");
        cli.setApellidoPaterno("Lopez");
        cli.setApellidoMaterno("Garcia");
        cli.setFechaNacimiento(LocalDate.of(1998, 5, 15));
        cli.setCurp("LOGP980515MMCPRL09");
        cli.setRfc("LOGP980515AA0");
        cli.setSexo("FEMENINO");
        cli.setNacionalidad("MEXICANA");
        cli.setEstadoCivil("SOLTERO");
        cli.setCorreo("paola@gmail.com");
        cli.setTelefonoMovil("5512345678");
        cli.setTelefonoAlternativo("5587654321");
        cli.setOcupacion("Ingeniera");
        cli.setEmpresa("Tech");
        cli.setIngresoMensual(new BigDecimal("30000.00"));
        cli.setActivo(true);
        cli.setFechaCreacion(LocalDateTime.now());

        Domicilio dom = new Domicilio();
        dom.setId(1);
        dom.setCalle("Insurgentes");
        dom.setNumeroExterior("100");
        dom.setNumeroInterior("1");
        dom.setColonia("Roma");
        dom.setMunicipio("Cuauhtemoc");
        dom.setEstado("CDMX");
        dom.setCodigoPostal("06700");
        dom.setPais("Mexico");

        cli.addDomicilio(dom);

        assertEquals(1, cli.getId());
        assertEquals("Paola", cli.getNombre());
        assertEquals("Sofia", cli.getSegundoNombre());
        assertEquals("Lopez", cli.getApellidoPaterno());
        assertEquals("Garcia", cli.getApellidoMaterno());
        assertEquals(LocalDate.of(1998, 5, 15), cli.getFechaNacimiento());
        assertEquals("LOGP980515MMCPRL09", cli.getCurp());
        assertEquals("LOGP980515AA0", cli.getRfc());
        assertEquals("FEMENINO", cli.getSexo());
        assertEquals("MEXICANA", cli.getNacionalidad());
        assertEquals("SOLTERO", cli.getEstadoCivil());
        assertEquals("paola@gmail.com", cli.getCorreo());
        assertEquals("5512345678", cli.getTelefonoMovil());
        assertEquals("5587654321", cli.getTelefonoAlternativo());
        assertEquals("Ingeniera", cli.getOcupacion());
        assertEquals("Tech", cli.getEmpresa());
        assertEquals(new BigDecimal("30000.00"), cli.getIngresoMensual());
        assertTrue(cli.getActivo());
        assertNotNull(cli.getFechaCreacion());
        assertEquals(1, cli.getDomicilios().size());

        assertEquals(cli, dom.getCliente());
        assertEquals(1, dom.getId());
        assertEquals("Insurgentes", dom.getCalle());
        assertEquals("100", dom.getNumeroExterior());
        assertEquals("1", dom.getNumeroInterior());
        assertEquals("Roma", dom.getColonia());
        assertEquals("Cuauhtemoc", dom.getMunicipio());
        assertEquals("CDMX", dom.getEstado());
        assertEquals("06700", dom.getCodigoPostal());
        assertEquals("Mexico", dom.getPais());

        // Lifecycle onCreate (when null)
        Cliente cliPersist = new Cliente();
        cliPersist.setActivo(null);
        cliPersist.onCreate();
        assertNotNull(cliPersist.getFechaCreacion());
        assertTrue(cliPersist.getActivo());

        // Lifecycle onCreate (when already set)
        Cliente cliSet = new Cliente();
        LocalDateTime dt = LocalDateTime.of(2026, 1, 1, 0, 0);
        cliSet.setFechaCreacion(dt);
        cliSet.setActivo(false);
        cliSet.onCreate();
        assertEquals(dt, cliSet.getFechaCreacion());
        assertFalse(cliSet.getActivo());
    }
}
