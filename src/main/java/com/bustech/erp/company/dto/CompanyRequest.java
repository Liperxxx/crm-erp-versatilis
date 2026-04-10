package com.bustech.erp.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRequest(

        @NotBlank(message = "Nome e obrigatorio.")
        @Size(max = 120, message = "Nome deve ter no maximo 120 caracteres.")
        String name,

        @NotBlank(message = "Slug e obrigatorio.")
        @Size(max = 80, message = "Slug deve ter no maximo 80 caracteres.")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug deve conter apenas letras minusculas, numeros e hifens.")
        String slug
) {}
