package com.nexbuy.modules.auth.controller;

import com.nexbuy.modules.auth.dto.*;
import com.nexbuy.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody @Valid OtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-registration-email")
    public ResponseEntity<Void> updateRegistrationEmail(@RequestBody @Valid UpdatePendingEmailRequest request) {
        authService.updatePendingRegistrationEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<AuthResponse> reset(@RequestBody @Valid ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}