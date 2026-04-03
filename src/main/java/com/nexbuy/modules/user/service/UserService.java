package com.nexbuy.modules.user.service;

import com.nexbuy.modules.user.dto.UserProfileDto;

import java.util.List;

public interface UserService {
    UserProfileDto.CheckoutProfileResponse getCheckoutProfile(String email);

    List<UserProfileDto.AddressSummary> getAddresses(String email);

    UserProfileDto.AddressSummary saveAddress(String email, Long addressId, UserProfileDto.UpsertAddressRequest request);

    UserProfileDto.AddressSummary setDefaultAddress(String email, Long addressId);
}
