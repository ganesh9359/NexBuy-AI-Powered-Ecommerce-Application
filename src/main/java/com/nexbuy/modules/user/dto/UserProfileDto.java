package com.nexbuy.modules.user.dto;

import java.util.List;

public class UserProfileDto {

    public record ProfileSummary(Long userId,
                                 String email,
                                 String firstName,
                                 String lastName) {
    }

    public record AddressSummary(Long id,
                                 String label,
                                 String line1,
                                 String line2,
                                 String city,
                                 String state,
                                 String postalCode,
                                 String country,
                                 boolean isDefault,
                                 String displayText) {
    }

    public record CheckoutProfileResponse(ProfileSummary profile,
                                          List<AddressSummary> addresses) {
    }

    public record UpsertAddressRequest(String label,
                                       String line1,
                                       String line2,
                                       String city,
                                       String state,
                                       String postalCode,
                                       String country,
                                       Boolean isDefault) {
    }
}
