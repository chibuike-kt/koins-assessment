package com.koins.controller;

import com.koins.dto.response.ApiResponse;
import com.koins.dto.response.TransactionResponse;
import com.koins.entity.Transaction;
import com.koins.entity.User;
import com.koins.exception.ResourceNotFoundException;
import com.koins.repository.TransactionRepository;
import com.koins.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<TransactionResponse> transactions = transactionRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Transactions fetched", transactions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Transaction not found");
        }

        return ResponseEntity.ok(ApiResponse.success("Transaction fetched", mapToResponse(transaction)));
    }

    private TransactionResponse mapToResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .userId(tx.getUser().getId())
                .walletId(tx.getWallet().getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .reference(tx.getReference())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
