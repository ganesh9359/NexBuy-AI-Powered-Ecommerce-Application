package com.nexbuy.modules.cart.service;

import com.nexbuy.modules.cart.dto.CartItemDto;

public interface CartService {
    CartItemDto.CartResponse getCart(String email);

    CartItemDto.CartResponse addItem(String email, CartItemDto.AddItemRequest request);

    CartItemDto.CartResponse updateItem(String email, Long itemId, CartItemDto.UpdateQuantityRequest request);

    CartItemDto.CartResponse removeItem(String email, Long itemId);

    CartItemDto.CartResponse clearCart(String email);
}
