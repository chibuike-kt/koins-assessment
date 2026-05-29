package com.koins.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundWalletRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum funding amount is 10000 kobo (NGN 100)")
    private BigDecimal amount;

    private String email;
}
