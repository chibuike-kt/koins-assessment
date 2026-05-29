package com.koins.controller;

import com.koins.dto.request.FundWalletRequest;
import com.koins.dto.response.ApiResponse;
import com.koins.dto.response.PaystackInitResponse;
import com.koins.dto.response.TransactionResponse;
import com.koins.dto.response.WalletResponse;
import com.koins.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<WalletResponse>> getBalance(
            @AuthenticationPrincipal UserDetails userDetails) {
        WalletResponse response = walletService.getWalletBalance(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wallet balance fetched", response));
    }

    @PostMapping("/fund")
    public ResponseEntity<ApiResponse<PaystackInitResponse>> fundWallet(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FundWalletRequest request) {
        PaystackInitResponse response = walletService.initiateFunding(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment initialized", response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TransactionResponse> response = walletService.getTransactionHistory(
                userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched", response));
    }
}
