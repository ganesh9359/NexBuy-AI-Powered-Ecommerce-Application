package com.nexbuy.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminReturnReviewRequest {

    @NotBlank
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
