package com.koins;

import com.koins.dto.request.LoginRequest;
import com.koins.dto.request.SignupRequest;
import com.koins.dto.response.AuthResponse;
import com.koins.entity.User;
import com.koins.entity.Wallet;
import com.koins.enums.AccountStatus;
import com.koins.enums.Role;
import com.koins.enums.WalletStatus;
import com.koins.exception.BadRequestException;
import com.koins.repository.UserRepository;
import com.koins.repository.WalletRepository;
import com.koins.security.JwtService;
import com.koins.service.EmailService;
import com.koins.service.impl.AuthServiceImpl;
import com.koins.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private EmailService emailService;
    @Mock private OtpUtil otpUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequest signupRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setFullName("Test User");
        signupRequest.setEmail("test@koins.com");
        signupRequest.setPhoneNumber("+2348012345678");
        signupRequest.setPassword("password123");
        signupRequest.setBvnNin("12345678901");

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
    }

    @Test
    void signup_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(walletRepository.save(any(Wallet.class))).thenReturn(Wallet.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .balance(BigDecimal.ZERO)
                .currency("NGN")
                .status(WalletStatus.ACTIVE)
                .build());

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mock_jwt_token");

        AuthResponse response = authService.signup(signupRequest);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        verify(userRepository).save(any(User.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void signup_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.signup(signupRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_duplicatePhone_throwsBadRequest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.signup(signupRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@koins.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mock_jwt_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
    }

    @Test
    void login_suspendedAccount_throwsBadRequest() {
        mockUser.setStatus(AccountStatus.SUSPENDED);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@koins.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }
}
