package com.koins.service;

import com.koins.dto.request.FundWalletRequest;
import com.koins.dto.response.PaystackInitResponse;
import com.koins.dto.response.TransactionResponse;
import com.koins.dto.response.WalletResponse;

import java.util.List;

public interface WalletService {
    WalletResponse getWalletBalance(String email);
    PaystackInitResponse initiateFunding(String email, FundWalletRequest request);
    List<TransactionResponse> getTransactionHistory(String email);
}
