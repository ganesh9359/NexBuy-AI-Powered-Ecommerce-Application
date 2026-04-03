package com.nexbuy.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {
    @NotBlank @Email
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = normalizeEmail(email); }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.replaceAll("\\s+", "").trim().toLowerCase();
    }
}