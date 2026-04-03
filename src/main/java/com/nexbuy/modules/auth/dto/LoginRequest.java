package com.nexbuy.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
    private String otp; // optional for step-2

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = normalizeEmail(email); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.replaceAll("\\s+", "").trim().toLowerCase();
    }
}