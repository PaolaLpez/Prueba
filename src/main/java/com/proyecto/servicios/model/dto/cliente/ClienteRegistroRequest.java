package com.proyecto.servicios.model.dto.cliente;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClienteRegistroRequest {

    @NotBlank(message = "El nombre es obligatorio y no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres (mínimo 3 letras)")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{3,50}$", message = "El nombre debe contener solo letras y al menos 3 caracteres")
    private String nombre;

    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{3,50}$", message = "El segundo nombre debe contener solo letras y al menos 3 caracteres si se proporciona")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio y no puede estar vacío")
    @Size(min = 3, max = 50, message = "El apellido paterno debe tener entre 3 y 50 caracteres (mínimo 3 letras)")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{3,50}$", message = "El apellido paterno debe contener solo letras y al menos 3 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio y no puede estar vacío")
    @Size(min = 3, max = 50, message = "El apellido materno debe tener entre 3 y 50 caracteres (mínimo 3 letras)")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{3,50}$", message = "El apellido materno debe contener solo letras y al menos 3 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria y no puede estar vacía")
    @Size(min = 18, max = 18, message = "La CURP debe tener exactamente 18 caracteres")
    @Pattern(regexp = "(?i)^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]\\d$", message = "El formato de la CURP es inválido (debe contener 18 caracteres en formato oficial)")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio y no puede estar vacío")
    @Size(min = 12, max = 13, message = "El RFC debe tener 12 o 13 caracteres")
    @Pattern(regexp = "(?i)^[A-ZÑ&]{3,4}\\d{6}[A-V1-9][A-Z1-9][0-9A]$", message = "El formato del RFC es inválido (debe contener 12 o 13 caracteres en formato oficial)")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio y debe ser MASCULINO, FEMENINO u OTRO")
    @Pattern(regexp = "(?i)^(MASCULINO|FEMENINO|OTRO)$", message = "El sexo debe ser exclusivamente MASCULINO, FEMENINO u OTRO")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Pattern(regexp = "(?i)^(MEXICANA|EXTRANJERA|ESTADOUNIDENSE|CANADIENSE|ESPANIOLA|ESPAÑOLA|COLOMBIANA|ARGENTINA|OTRA)$", 
             message = "La nacionalidad debe ser una opción válida del catálogo (MEXICANA, EXTRANJERA, ESTADOUNIDENSE, CANADIENSE, ESPANIOLA, COLOMBIANA, ARGENTINA, OTRA)")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio y no puede estar vacío")
    @Pattern(regexp = "(?i)^(SOLTERO|CASADO|DIVORCIADO|VIUDO|UNION LIBRE|UNION_LIBRE)$", 
             message = "El estado civil debe ser una opción válida del catálogo (SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION LIBRE)")
    private String estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio y no puede estar vacío")
    @Email(message = "El formato del correo electrónico es inválido")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "El formato del correo electrónico es inválido (ejemplo: usuario@dominio.com)")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio y no puede estar vacío")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil solo debe contener números (exactamente 10 dígitos, sin letras ni caracteres extraños)")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El teléfono alternativo solo debe contener números (exactamente 10 dígitos, sin letras ni caracteres extraños)")
    private String telefonoAlternativo;

    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{2,100}$", message = "La ocupación solo debe contener letras y espacios (sin números ni caracteres extraños)")
    private String ocupacion;

    @Pattern(regexp = "^$|^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s.,&-_]{2,100}$", message = "La empresa no debe contener caracteres extraños")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual debe tener un formato válido con máximo 2 decimales")
    private BigDecimal ingresoMensual;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El saldo inicial debe tener un formato válido con máximo 2 decimales")
    private BigDecimal saldoInicial;

    @Valid
    @NotNull(message = "El domicilio es obligatorio")
    private DomicilioDto domicilio;

    @Pattern(
        regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s])\\S{8,100}$",
        message = "La contraseña debe ser segura: mínimo 8 caracteres, al menos una letra mayúscula, una minúscula, un número y un carácter especial (@, $, !, %, *, ?, &, #, etc.)"
    )
    private String password;
}
