package com.nexbuy.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminOrderStatusRequest {
    @NotBlank
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}