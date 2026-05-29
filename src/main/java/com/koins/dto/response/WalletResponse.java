package com.koins.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.koins.enums.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID id;
    private UUID userId;

    @JsonIgnore
    private BigDecimal balanceKobo;

    private String currency;
    private WalletStatus status;
    private LocalDateTime createdAt;

    public BigDecimal getBalance() {
        if (balanceKobo == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return balanceKobo.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
