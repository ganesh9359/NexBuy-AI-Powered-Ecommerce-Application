package com.nexbuy.modules.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class ResendOtpRequest {
    @NotNull
    private Long userId;
    @NotBlank
    private String purpose;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
