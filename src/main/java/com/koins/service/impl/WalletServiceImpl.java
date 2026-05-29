package com.koins.service.impl;

import com.koins.dto.request.FundWalletRequest;
import com.koins.dto.response.PaystackInitResponse;
import com.koins.dto.response.TransactionResponse;
import com.koins.dto.response.WalletResponse;
import com.koins.entity.Transaction;
import com.koins.entity.User;
import com.koins.entity.Wallet;
import com.koins.enums.TransactionStatus;
import com.koins.enums.TransactionType;
import com.koins.enums.WalletStatus;
import com.koins.exception.BadRequestException;
import com.koins.exception.ResourceNotFoundException;
import com.koins.repository.TransactionRepository;
import com.koins.repository.UserRepository;
import com.koins.repository.WalletRepository;
import com.koins.service.PaystackService;
import com.koins.service.WalletService;
import com.koins.util.ReferenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PaystackService paystackService;
    private final ReferenceUtil referenceUtil;

    @Override
    public WalletResponse getWalletBalance(String email) {
        User user = getUser(email);
        Wallet wallet = getWallet(user);
        return mapToWalletResponse(wallet);
    }

    @Override
    @Transactional
    public PaystackInitResponse initiateFunding(String email, FundWalletRequest request) {
        User user = getUser(email);
        Wallet wallet = getWallet(user);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Wallet is not active");
        }

        String reference = referenceUtil.generateReference();
        String userEmail = request.getEmail() != null ? request.getEmail() : user.getEmail();

        Transaction transaction = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .type(TransactionType.CREDIT)
                .amount(request.getAmount())
                .status(TransactionStatus.PENDING)
                .reference(reference)
                .description("Wallet funding via Paystack")
                .build();

        transactionRepository.save(transaction);

        log.info("Funding initiated for user: {} amount: {} kobo", email, request.getAmount());

        return paystackService.initializePayment(userEmail, request.getAmount(), reference);
    }

    @Override
    public List<TransactionResponse> getTransactionHistory(String email) {
        User user = getUser(email);
        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Wallet getWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUser().getId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    private TransactionResponse mapToTransactionResponse(Transaction tx) {
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
