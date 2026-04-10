package com.bustech.erp.financial.dto;

import com.bustech.erp.common.enums.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

    @NotNull(message = "Tipo é obrigatório.")
    TransactionType type,

    @NotBlank(message = "Descrição é obrigatória.")
    @Size(max = 255)
    String description,

    @NotNull(message = "Valor é obrigatório.")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero.")
    @Digits(integer = 16, fraction = 2)
    BigDecimal amount,

    @NotNull(message = "Data de vencimento é obrigatória.")
    LocalDate dueDate,

    @Size(max = 100)
    String category,

    @Size(max = 100)
    String costCenter,

    @Size(max = 255)
    String notes
) {}
