package com.nexbuy.modules.cart.controller;

import com.nexbuy.modules.cart.dto.CartItemDto;
import com.nexbuy.modules.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartItemDto.CartResponse> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(authentication.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemDto.CartResponse> addItem(Authentication authentication,
                                                            @RequestBody CartItemDto.AddItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(authentication.getName(), request));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartItemDto.CartResponse> updateItem(Authentication authentication,
                                                               @PathVariable Long itemId,
                                                               @RequestBody CartItemDto.UpdateQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateItem(authentication.getName(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartItemDto.CartResponse> removeItem(Authentication authentication,
                                                               @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(authentication.getName(), itemId));
    }

    @DeleteMapping
    public ResponseEntity<CartItemDto.CartResponse> clear(Authentication authentication) {
        return ResponseEntity.ok(cartService.clearCart(authentication.getName()));
    }
}
