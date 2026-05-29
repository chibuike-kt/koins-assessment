package com.koins.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaystackInitResponse {
    private String authorizationUrl;
    private String accessCode;
    private String reference;
}
