package com.proyecto.servicios.model.dto.cliente;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomicilioDto {

    @NotBlank(message = "La calle es obligatoria")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s.,#-]{2,100}$", message = "La calle no debe contener caracteres extraños")
    @Size(max = 100, message = "La calle no puede exceder 100 caracteres")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9\\s#-]{1,20}$", message = "El número exterior no debe contener caracteres extraños")
    @Size(max = 20, message = "El número exterior no puede exceder 20 caracteres")
    private String numeroExterior;

    @Pattern(regexp = "^$|^[a-zA-Z0-9\\s#-]{1,20}$", message = "El número interior no debe contener caracteres extraños")
    @Size(max = 20, message = "El número interior no puede exceder 20 caracteres")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s.,#-]{2,100}$", message = "La colonia no debe contener caracteres extraños")
    @Size(max = 100, message = "La colonia no puede exceder 100 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{2,100}$", message = "El municipio solo debe contener letras y espacios (sin números ni caracteres extraños)")
    @Size(max = 100, message = "El municipio no puede exceder 100 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{2,100}$", message = "El estado solo debe contener letras y espacios (sin números ni caracteres extraños)")
    @Size(max = 100, message = "El estado no puede exceder 100 caracteres")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal solo debe contener números (exactamente 5 dígitos, sin letras ni caracteres extraños)")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{2,50}$", message = "El país solo debe contener letras y espacios (sin números ni caracteres extraños)")
    @Size(max = 50, message = "El país no puede exceder 50 caracteres")
    private String pais;
}
