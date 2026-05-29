package com.koins.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is NGN 1,000")
    private BigDecimal loanAmount;

    @NotNull(message = "Loan duration is required")
    @Min(value = 7, message = "Minimum loan duration is 7 days")
    @Max(value = 365, message = "Maximum loan duration is 365 days")
    private Integer loanDuration;
}
