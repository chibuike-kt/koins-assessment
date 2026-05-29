package com.koins.service;

import com.koins.dto.request.*;
import com.koins.dto.response.AuthResponse;
import com.koins.dto.response.UserResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void resendOtp(ResendOtpRequest request);
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    UserResponse getProfile(String email);
}
