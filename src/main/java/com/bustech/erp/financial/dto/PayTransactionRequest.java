package com.bustech.erp.financial.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayTransactionRequest(

    @NotNull(message = "Valor pago é obrigatório.")
    @DecimalMin(value = "0.01", message = "Valor pago deve ser maior que zero.")
    @Digits(integer = 16, fraction = 2)
    BigDecimal paidAmount,

    @NotNull(message = "Data de pagamento é obrigatória.")
    LocalDate paymentDate
) {}
