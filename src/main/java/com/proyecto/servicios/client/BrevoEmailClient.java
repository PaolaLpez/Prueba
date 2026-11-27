package com.proyecto.servicios.client;

import com.proyecto.servicios.model.dto.brevo.BrevoEmailRequest;
import com.proyecto.servicios.model.dto.brevo.BrevoEmailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente OpenFeign para la API transaccional v3 de Brevo (Sendinblue).
 * Endpoint oficial: POST https://api.brevo.com/v3/smtp/email
 */
@FeignClient(name = "brevoEmailClient", url = "${brevo.api.url:https://api.brevo.com/v3}")
public interface BrevoEmailClient {

    @PostMapping(value = "/smtp/email", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    BrevoEmailResponse sendEmail(
            @RequestHeader("api-key") String apiKey,
            @RequestBody BrevoEmailRequest request
    );
}
