package com.nexbuy.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminReturnUpdateRequest {
    
    @NotBlank(message = "Action is required")
    private String action; // approve, reject, accept, complete, cancel

    private String note;

    public AdminReturnUpdateRequest() {
    }

    public AdminReturnUpdateRequest(String action, String note) {
        this.action = action;
        this.note = note;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
