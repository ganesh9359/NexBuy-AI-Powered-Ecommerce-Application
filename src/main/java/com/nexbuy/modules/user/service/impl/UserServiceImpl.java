package com.nexbuy.modules.user.service.impl;

import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.commerce.CommerceSupport;
import com.nexbuy.modules.user.dto.UserProfileDto;
import com.nexbuy.modules.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final JdbcTemplate jdbcTemplate;
    private final CommerceSupport commerceSupport;

    public UserServiceImpl(JdbcTemplate jdbcTemplate,
                           CommerceSupport commerceSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.commerceSupport = commerceSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto.CheckoutProfileResponse getCheckoutProfile(String email) {
        long userId = commerceSupport.requireUserId(email);
        return new UserProfileDto.CheckoutProfileResponse(
                commerceSupport.loadProfileSummary(userId),
                commerceSupport.loadAddresses(userId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDto.AddressSummary> getAddresses(String email) {
        long userId = commerceSupport.requireUserId(email);
        return commerceSupport.loadAddresses(userId);
    }

    @Override
    public UserProfileDto.AddressSummary saveAddress(String email, Long addressId, UserProfileDto.UpsertAddressRequest request) {
        long userId = commerceSupport.requireUserId(email);
        validateRequest(request);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || commerceSupport.loadAddresses(userId).isEmpty();

        if (makeDefault) {
            jdbcTemplate.update("update addresses set is_default = false where user_id = ?", userId);
        }

        if (addressId == null) {
            jdbcTemplate.update("""
                    insert into addresses (user_id, label, line1, line2, city, state, postal_code, country, is_default, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                    """, userId, blankToNull(request.label()), request.line1().trim(), blankToNull(request.line2()),
                    request.city().trim(), request.state().trim(), request.postalCode().trim(), request.country().trim(), makeDefault);
            Long newId = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
            return commerceSupport.loadAddresses(userId).stream()
                    .filter(address -> address.id().equals(newId))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Address could not be saved", HttpStatus.INTERNAL_SERVER_ERROR));
        }

        int updated = jdbcTemplate.update("""
                update addresses
                set label = ?, line1 = ?, line2 = ?, city = ?, state = ?, postal_code = ?, country = ?, is_default = ?, updated_at = current_timestamp
                where id = ? and user_id = ?
                """, blankToNull(request.label()), request.line1().trim(), blankToNull(request.line2()), request.city().trim(), request.state().trim(),
                request.postalCode().trim(), request.country().trim(), makeDefault, addressId, userId);
        if (updated == 0) {
            throw new CustomException("Address not found", HttpStatus.NOT_FOUND);
        }
        return commerceSupport.loadAddresses(userId).stream()
                .filter(address -> address.id().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new CustomException("Address not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public UserProfileDto.AddressSummary setDefaultAddress(String email, Long addressId) {
        long userId = commerceSupport.requireUserId(email);
        commerceSupport.requireAddress(userId, addressId);
        jdbcTemplate.update("update addresses set is_default = false where user_id = ?", userId);
        jdbcTemplate.update("update addresses set is_default = true, updated_at = current_timestamp where id = ? and user_id = ?", addressId, userId);
        return commerceSupport.loadAddresses(userId).stream()
                .filter(address -> address.id().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new CustomException("Address not found", HttpStatus.NOT_FOUND));
    }

    private void validateRequest(UserProfileDto.UpsertAddressRequest request) {
        if (request == null
                || isBlank(request.line1())
                || isBlank(request.city())
                || isBlank(request.state())
                || isBlank(request.postalCode())
                || isBlank(request.country())) {
            throw new CustomException("Please complete all required address fields", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}