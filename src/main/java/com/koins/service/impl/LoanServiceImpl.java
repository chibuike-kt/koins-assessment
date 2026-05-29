package com.koins.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koins.dto.request.LoanRepaymentRequest;
import com.koins.dto.request.LoanRequest;
import com.koins.dto.response.LoanResponse;
import com.koins.entity.Loan;
import com.koins.entity.Transaction;
import com.koins.entity.User;
import com.koins.entity.Wallet;
import com.koins.enums.LoanStatus;
import com.koins.enums.TransactionStatus;
import com.koins.enums.TransactionType;
import com.koins.exception.BadRequestException;
import com.koins.exception.ResourceNotFoundException;
import com.koins.repository.LoanRepository;
import com.koins.repository.TransactionRepository;
import com.koins.repository.UserRepository;
import com.koins.repository.WalletRepository;
import com.koins.service.EmailService;
import com.koins.service.LoanService;
import com.koins.util.ReferenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private static final BigDecimal INTEREST_RATE = new BigDecimal("5.00");
    private static final BigDecimal LOAN_MULTIPLIER = new BigDecimal("3");

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;
    private final ReferenceUtil referenceUtil;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LoanResponse applyForLoan(String email, LoanRequest request) {
        User user = getUser(email);
        Wallet wallet = getWallet(user);

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Wallet must be funded before applying for a loan");
        }

        BigDecimal maxLoanAmount = wallet.getBalance().multiply(LOAN_MULTIPLIER);
        if (request.getLoanAmount().compareTo(maxLoanAmount) > 0) {
            throw new BadRequestException(
                    "Loan amount exceeds maximum allowed. Max: " + maxLoanAmount + " kobo");
        }

        List<Loan> activeLoans = loanRepository.findByUserId(user.getId()).stream()
                .filter(l -> l.getStatus() == LoanStatus.DISBURSED)
                .collect(Collectors.toList());

        if (!activeLoans.isEmpty()) {
            throw new BadRequestException("You have an active loan. Repay it before applying for a new one");
        }

        BigDecimal interest = request.getLoanAmount()
                .multiply(INTEREST_RATE)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        BigDecimal totalRepayable = request.getLoanAmount().add(interest);

        LocalDateTime dueDate = LocalDateTime.now().plusDays(request.getLoanDuration());
        String repaymentSchedule = buildRepaymentSchedule(
                request.getLoanAmount(), totalRepayable, dueDate, request.getLoanDuration());

        Loan loan = Loan.builder()
                .user(user)
                .loanAmount(request.getLoanAmount())
                .interestRate(INTEREST_RATE)
                .loanDuration(request.getLoanDuration())
                .status(LoanStatus.PENDING)
                .repaymentSchedule(repaymentSchedule)
                .dueDate(dueDate)
                .build();

        loanRepository.save(loan);
        log.info("Loan application submitted by: {} amount: {} kobo", email, request.getLoanAmount());

        return mapToLoanResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse approveLoan(UUID loanId) {
        Loan loan = getLoan(loanId);

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BadRequestException("Only pending loans can be approved");
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());
        loanRepository.save(loan);

        emailService.sendLoanApprovalEmail(
                loan.getUser().getEmail(),
                loan.getUser().getFullName(),
                loan.getLoanAmount().toString()
        );

        log.info("Loan approved: {}", loanId);
        return mapToLoanResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse disburseLoan(UUID loanId) {
        Loan loan = getLoan(loanId);

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BadRequestException("Only approved loans can be disbursed");
        }

        Wallet wallet = getWallet(loan.getUser());
        wallet.setBalance(wallet.getBalance().add(loan.getLoanAmount()));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .user(loan.getUser())
                .wallet(wallet)
                .type(TransactionType.LOAN_DISBURSEMENT)
                .amount(loan.getLoanAmount())
                .status(TransactionStatus.SUCCESS)
                .reference(referenceUtil.generateReference())
                .description("Loan disbursement - Loan ID: " + loanId)
                .build();

        transactionRepository.save(transaction);

        loan.setStatus(LoanStatus.DISBURSED);
        loan.setDisbursedAt(LocalDateTime.now());
        loanRepository.save(loan);

        log.info("Loan disbursed: {} amount: {} kobo", loanId, loan.getLoanAmount());
        return mapToLoanResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse repayLoan(String email, LoanRepaymentRequest request) {
        User user = getUser(email);
        Wallet wallet = getWallet(user);
        Loan loan = getLoan(request.getLoanId());

        if (!loan.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Loan does not belong to this user");
        }

        if (loan.getStatus() != LoanStatus.DISBURSED) {
            throw new BadRequestException("Only disbursed loans can be repaid");
        }

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient wallet balance for repayment");
        }

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .type(TransactionType.REPAYMENT)
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .reference(referenceUtil.generateReference())
                .description("Loan repayment - Loan ID: " + request.getLoanId())
                .build();

        transactionRepository.save(transaction);

        BigDecimal interest = loan.getLoanAmount()
                .multiply(INTEREST_RATE)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        BigDecimal totalRepayable = loan.getLoanAmount().add(interest);

        if (request.getAmount().compareTo(totalRepayable) >= 0) {
            loan.setStatus(LoanStatus.REPAID);
            loanRepository.save(loan);
            emailService.sendRepaymentSuccessEmail(
                    user.getEmail(), user.getFullName(), request.getAmount().toString());
            log.info("Loan fully repaid: {}", request.getLoanId());
        } else {
            log.info("Partial repayment of {} kobo for loan: {}",
                    request.getAmount(), request.getLoanId());
        }

        return mapToLoanResponse(loan);
    }

    @Override
    public LoanResponse getLoanById(UUID loanId) {
        return mapToLoanResponse(getLoan(loanId));
    }

    @Override
    public List<LoanResponse> getUserLoans(String email) {
        User user = getUser(email);
        return loanRepository.findByUser(user).stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanResponse> getAllLoans() {
        return loanRepository.findAll().stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    private String buildRepaymentSchedule(BigDecimal principal, BigDecimal totalRepayable,
                                           LocalDateTime dueDate, int durationDays) {
        try {
            List<Map<String, Object>> schedule = new ArrayList<>();
            Map<String, Object> entry = new HashMap<>();
            entry.put("installment", 1);
            entry.put("principal_kobo", principal.longValue());
            entry.put("total_repayable_kobo", totalRepayable.longValue());
            entry.put("due_date", dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            entry.put("duration_days", durationDays);
            schedule.add(entry);
            return objectMapper.writeValueAsString(schedule);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private LoanResponse mapToLoanResponse(Loan loan) {
        BigDecimal interest = loan.getLoanAmount()
                .multiply(loan.getInterestRate())
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        BigDecimal totalRepayable = loan.getLoanAmount().add(interest);

        return LoanResponse.builder()
                .id(loan.getId())
                .userId(loan.getUser().getId())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .totalRepayable(totalRepayable)
                .loanDuration(loan.getLoanDuration())
                .status(loan.getStatus())
                .repaymentSchedule(loan.getRepaymentSchedule())
                .dueDate(loan.getDueDate())
                .approvedAt(loan.getApprovedAt())
                .disbursedAt(loan.getDisbursedAt())
                .createdAt(loan.getCreatedAt())
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Wallet getWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private Loan getLoan(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }
}
