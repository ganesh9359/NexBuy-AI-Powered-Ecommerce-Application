package com.nexbuy.modules.auth.service;

import com.nexbuy.modules.auth.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse verifyOtp(OtpRequest request);
    void resendOtp(ResendOtpRequest request);
    void updatePendingRegistrationEmail(UpdatePendingEmailRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    AuthResponse resetPassword(ResetPasswordRequest request);
}