package com.nexbuy.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String otp;
    @NotBlank @Size(min = 6)
    private String newPassword;
    @NotBlank @Size(min = 6)
    private String confirmPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = normalizeEmail(email); }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.replaceAll("\\s+", "").trim().toLowerCase();
    }
}