package com.koins.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koins.config.AppProperties;
import com.koins.entity.Transaction;
import com.koins.entity.Wallet;
import com.koins.enums.TransactionStatus;
import com.koins.enums.TransactionType;
import com.koins.repository.TransactionRepository;
import com.koins.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final AppProperties appProperties;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/paystack")
    @Transactional
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestHeader("x-paystack-signature") String signature,
            @RequestBody String payload) {

        if (!verifySignature(payload, signature)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("event").asText();

            log.info("Paystack webhook received: {}", eventType);

            if ("charge.success".equals(eventType)) {
                handleChargeSuccess(event.get("data"));
            }

            return ResponseEntity.ok("OK");

        } catch (Exception ex) {
            log.error("Webhook processing error: {}", ex.getMessage());
            return ResponseEntity.ok("OK");
        }
    }

    private void handleChargeSuccess(JsonNode data) {
        String reference = data.get("reference").asText();
        long amountInKobo = data.get("amount").asLong();
        String paystackStatus = data.get("status").asText();

        if (!"success".equalsIgnoreCase(paystackStatus)) {
            log.warn("Charge not successful for reference: {}", reference);
            return;
        }

        Optional<Transaction> txOpt = transactionRepository.findByReference(reference);

        if (txOpt.isEmpty()) {
            log.warn("Transaction not found for reference: {}", reference);
            return;
        }

        Transaction transaction = txOpt.get();

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transaction already processed: {}", reference);
            return;
        }

        if (transaction.getType() != TransactionType.CREDIT) {
            log.warn("Unexpected transaction type for webhook: {}", transaction.getType());
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(amountInKobo);
        Wallet wallet = transaction.getWallet();
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        log.info("Wallet credited {} kobo for user: {} reference: {}",
                amountInKobo, transaction.getUser().getEmail(), reference);
    }

    private boolean verifySignature(String payload, String signature) {
        try {
            String secretKey = appProperties.getPaystack().getSecretKey();
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equalsIgnoreCase(signature);
        } catch (Exception ex) {
            log.error("Signature verification error: {}", ex.getMessage());
            return false;
        }
    }
}
