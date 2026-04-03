package com.nexbuy.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 6)
    private String password;
    @NotBlank @Size(min = 6)
    private String confirmPassword;
    private String phone;
    private String firstName;
    private String lastName;
    private String line1;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = normalizeEmail(email); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.replaceAll("\\s+", "").trim().toLowerCase();
    }
}