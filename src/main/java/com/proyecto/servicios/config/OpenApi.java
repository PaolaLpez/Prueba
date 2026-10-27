package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración global de OpenAPI 3 / Swagger para documentar el ecosistema de pagos,
 * esquemas de seguridad Bearer Token y los flujos transaccionales con GestoPago y Brevo.
 */
@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Servicios Financieros y Procesamiento Transaccional de Pagos")
                        .version("1.0.0")
                        .description("Arquitectura de pagos con alta seguridad y concurrencia:\n" +
                                "- **Doble Factor de Autenticación (2FA)**: Generación de OTP de 6 dígitos con entrega real vía Brevo API y comprobantes digitales oficiales.\n" +
                                "- **Validación Estricta de Catálogo**: Comprobación de existencia de servicio, producto, estado activo y coincidencia exacta de montos para prevenir manipulación.\n" +
                                "- **Control de Concurrencia e Idempotencia**: Prevención de cobros duplicados por referencia única y bloqueo pesimista de balance en base de datos (`PESSIMISTIC_WRITE`).\n" +
                                "- **Saga Transaccional en 2 Fases**: Retención preventiva de fondos antes de llamar al proveedor externo para blindar la consistencia financiera.\n" +
                                "- **Integración GestoPago sendTx.do**: Consumo seguro mediante token Bearer enmascarado y gestión declarativa de timeouts sin asunción de fallos (`PENDIENTE_CONCILIACION`).\n" +
                                "- **Manejo Centralizado de Excepciones**: Respuestas semánticas estandarizadas con `@RestControllerAdvice`.")
                        .contact(new Contact()
                                .name("Soporte de Integración Transaccional")
                                .email("soporte@serviciosfinancieros.com"))
                        .license(new License()
                                .name("Uso Interno Empresarial")
                                .url("https://gestopago.portalventas.net")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Entorno Local de Pruebas")
                ))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token Bearer utilizado para autenticar las peticiones contra el backend de pagos y GestoPago.")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}