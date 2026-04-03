package com.nexbuy.modules.user.controller;

import com.nexbuy.modules.user.dto.UserProfileDto;
import com.nexbuy.modules.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/checkout-profile")
    public ResponseEntity<UserProfileDto.CheckoutProfileResponse> getCheckoutProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getCheckoutProfile(authentication.getName()));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<UserProfileDto.AddressSummary>> getAddresses(Authentication authentication) {
        return ResponseEntity.ok(userService.getAddresses(authentication.getName()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<UserProfileDto.AddressSummary> createAddress(Authentication authentication,
                                                                       @RequestBody UserProfileDto.UpsertAddressRequest request) {
        return ResponseEntity.ok(userService.saveAddress(authentication.getName(), null, request));
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<UserProfileDto.AddressSummary> updateAddress(Authentication authentication,
                                                                       @PathVariable Long addressId,
                                                                       @RequestBody UserProfileDto.UpsertAddressRequest request) {
        return ResponseEntity.ok(userService.saveAddress(authentication.getName(), addressId, request));
    }

    @PatchMapping("/addresses/{addressId}/default")
    public ResponseEntity<UserProfileDto.AddressSummary> setDefault(Authentication authentication,
                                                                    @PathVariable Long addressId) {
        return ResponseEntity.ok(userService.setDefaultAddress(authentication.getName(), addressId));
    }
}
