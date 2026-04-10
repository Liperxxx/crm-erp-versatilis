package com.bustech.erp.company.dto;

import com.bustech.erp.common.enums.UserRole;
import jakarta.validation.constraints.*;

public record CreateUserRequest(

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100)
    String name,

    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    @Size(max = 150)
    String email,

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres.")
    String password,

    @NotNull(message = "Perfil é obrigatório.")
    UserRole role,

    @NotNull(message = "ID da empresa é obrigatório.")
    Long companyId
) {}
