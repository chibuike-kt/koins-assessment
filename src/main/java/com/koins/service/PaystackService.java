package com.koins.service;

import com.koins.config.AppProperties;
import com.koins.dto.response.PaystackInitResponse;
import com.koins.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackService {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public PaystackInitResponse initializePayment(String email, BigDecimal amountInKobo, String reference) {
        String url = appProperties.getPaystack().getBaseUrl() + "/transaction/initialize";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(appProperties.getPaystack().getSecretKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("amount", amountInKobo.longValue());
        body.put("reference", reference);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !(Boolean) responseBody.get("status")) {
                throw new AppException("Paystack initialization failed", HttpStatus.BAD_GATEWAY);
            }

            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

            return PaystackInitResponse.builder()
                    .authorizationUrl((String) data.get("authorization_url"))
                    .accessCode((String) data.get("access_code"))
                    .reference((String) data.get("reference"))
                    .build();

        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Paystack initialization error: {}", ex.getMessage());
            throw new AppException("Payment gateway error", HttpStatus.BAD_GATEWAY);
        }
    }

    public boolean verifyPayment(String reference) {
        String url = appProperties.getPaystack().getBaseUrl() + "/transaction/verify/" + reference;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(appProperties.getPaystack().getSecretKey());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !(Boolean) responseBody.get("status")) {
                return false;
            }

            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            String paystackStatus = (String) data.get("status");
            return "success".equalsIgnoreCase(paystackStatus);

        } catch (Exception ex) {
            log.error("Paystack verification error: {}", ex.getMessage());
            return false;
        }
    }
}
