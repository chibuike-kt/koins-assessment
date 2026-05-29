package com.koins.dto.response;

import com.koins.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    private UUID id;
    private UUID userId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private BigDecimal totalRepayable;
    private Integer loanDuration;
    private LoanStatus status;
    private String repaymentSchedule;
    private LocalDateTime dueDate;
    private LocalDateTime approvedAt;
    private LocalDateTime disbursedAt;
    private LocalDateTime createdAt;
}
