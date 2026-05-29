package com.koins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koins.dto.request.LoanRequest;
import com.koins.dto.response.LoanResponse;
import com.koins.entity.Loan;
import com.koins.entity.User;
import com.koins.entity.Wallet;
import com.koins.enums.AccountStatus;
import com.koins.enums.LoanStatus;
import com.koins.enums.Role;
import com.koins.enums.WalletStatus;
import com.koins.exception.BadRequestException;
import com.koins.repository.LoanRepository;
import com.koins.repository.TransactionRepository;
import com.koins.repository.UserRepository;
import com.koins.repository.WalletRepository;
import com.koins.service.EmailService;
import com.koins.service.impl.LoanServiceImpl;
import com.koins.util.ReferenceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock private LoanRepository loanRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private EmailService emailService;
    @Mock private ReferenceUtil referenceUtil;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private LoanServiceImpl loanService;

    private User mockUser;
    private Wallet mockWallet;
    private LoanRequest loanRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@koins.com")
                .phoneNumber("+2348012345678")
                .password("hashed_password")
                .bvnNin("12345678901")
                .status(AccountStatus.ACTIVE)
                .role(Role.USER)
                .build();

        mockWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .balance(new BigDecimal("500000"))
                .currency("NGN")
                .status(WalletStatus.ACTIVE)
                .build();

        loanRequest = new LoanRequest();
        loanRequest.setLoanAmount(new BigDecimal("100000"));
        loanRequest.setLoanDuration(30);
    }

    @Test
    void applyForLoan_success() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUser(any(User.class))).thenReturn(Optional.of(mockWallet));
        when(loanRepository.findByUserId(any(UUID.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        LoanResponse response = loanService.applyForLoan("test@koins.com", loanRequest);

        assertNotNull(response);
        assertEquals(new BigDecimal("100000"), response.getLoanAmount());
        assertEquals(LoanStatus.PENDING, response.getStatus());
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void applyForLoan_emptyWallet_throwsBadRequest() {
        mockWallet.setBalance(BigDecimal.ZERO);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUser(any(User.class))).thenReturn(Optional.of(mockWallet));

        assertThrows(BadRequestException.class,
                () -> loanService.applyForLoan("test@koins.com", loanRequest));
    }

    @Test
    void applyForLoan_exceedsMaxAmount_throwsBadRequest() {
        loanRequest.setLoanAmount(new BigDecimal("2000000"));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUser(any(User.class))).thenReturn(Optional.of(mockWallet));

        assertThrows(BadRequestException.class,
                () -> loanService.applyForLoan("test@koins.com", loanRequest));
    }

    @Test
    void approveLoan_success() {
        Loan pendingLoan = Loan.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .loanAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("5.00"))
                .loanDuration(30)
                .status(LoanStatus.PENDING)
                .build();

        when(loanRepository.findById(any(UUID.class))).thenReturn(Optional.of(pendingLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendLoanApprovalEmail(anyString(), anyString(), anyString());

        LoanResponse response = loanService.approveLoan(pendingLoan.getId());

        assertEquals(LoanStatus.APPROVED, response.getStatus());
        assertNotNull(response.getApprovedAt());
    }

    @Test
    void approveLoan_notPending_throwsBadRequest() {
        Loan approvedLoan = Loan.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .loanAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("5.00"))
                .status(LoanStatus.APPROVED)
                .build();

        when(loanRepository.findById(any(UUID.class))).thenReturn(Optional.of(approvedLoan));

        assertThrows(BadRequestException.class,
                () -> loanService.approveLoan(approvedLoan.getId()));
    }
}
