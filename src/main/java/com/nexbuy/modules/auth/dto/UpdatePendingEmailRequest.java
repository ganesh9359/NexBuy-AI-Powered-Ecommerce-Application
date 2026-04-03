package com.nexbuy.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdatePendingEmailRequest {
    @NotNull
    private Long userId;

    @NotBlank
    @Email
    private String email;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.replaceAll("\\s+", "").trim().toLowerCase();
    }
}