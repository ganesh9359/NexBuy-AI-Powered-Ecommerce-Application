package com.nexbuy.modules.auth.dto;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private boolean requiresOtp;
    private Long userId;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String token, String refreshToken, String email) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.email = email;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isRequiresOtp() { return requiresOtp; }
    public void setRequiresOtp(boolean requiresOtp) { this.requiresOtp = requiresOtp; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
