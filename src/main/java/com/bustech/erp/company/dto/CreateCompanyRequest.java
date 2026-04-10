package com.bustech.erp.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100)
    String name,

    @NotBlank(message = "CNPJ é obrigatório.")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos numéricos.")
    String cnpj,

    @Size(max = 100)
    String tradeName,

    @Email(message = "E-mail inválido.")
    @Size(max = 150)
    String email,

    @Size(max = 20)
    String phone
) {}
