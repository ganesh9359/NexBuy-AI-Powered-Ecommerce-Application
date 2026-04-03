package com.nexbuy.modules.cart.service.impl;

import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.cart.dto.CartItemDto;
import com.nexbuy.modules.cart.service.CartService;
import com.nexbuy.modules.commerce.CommerceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final JdbcTemplate jdbcTemplate;
    private final CommerceSupport commerceSupport;

    public CartServiceImpl(JdbcTemplate jdbcTemplate,
                           CommerceSupport commerceSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.commerceSupport = commerceSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public CartItemDto.CartResponse getCart(String email) {
        long userId = commerceSupport.requireUserId(email);
        return commerceSupport.loadCart(userId);
    }

    @Override
    public CartItemDto.CartResponse addItem(String email, CartItemDto.AddItemRequest request) {
        long userId = commerceSupport.requireUserId(email);
        int quantity = sanitizeQuantity(request == null ? null : request.quantity());
        CommerceSupport.VariantRecord variant = commerceSupport.requireVariantBySku(request == null ? null : request.sku());
        if (!variant.canPurchase(quantity)) {
            throw new CustomException("Only " + variant.stockQty() + " unit(s) available for " + variant.title(), HttpStatus.CONFLICT);
        }

        long cartId = commerceSupport.getOrCreateActiveCartId(userId);
        Long existingItemId = jdbcTemplate.query("""
                select id
                from cart_items
                where cart_id = ? and variant_id = ?
                limit 1
                """, rs -> rs.next() ? rs.getLong("id") : null, cartId, variant.variantId());

        if (existingItemId != null) {
            Integer currentQty = jdbcTemplate.queryForObject("select qty from cart_items where id = ?", Integer.class, existingItemId);
            int nextQty = (currentQty == null ? 0 : currentQty) + quantity;
            if (!variant.canPurchase(nextQty)) {
                throw new CustomException("Only " + variant.stockQty() + " unit(s) available for " + variant.title(), HttpStatus.CONFLICT);
            }
            jdbcTemplate.update("""
                    update cart_items
                    set qty = ?, price_cents_snapshot = ?, currency = ?, updated_at = current_timestamp
                    where id = ?
                    """, nextQty, variant.priceCents(), variant.currency(), existingItemId);
        } else {
            jdbcTemplate.update("""
                    insert into cart_items (cart_id, variant_id, qty, price_cents_snapshot, currency, created_at, updated_at)
                    values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                    """, cartId, variant.variantId(), quantity, variant.priceCents(), variant.currency());
        }

        jdbcTemplate.update("update carts set updated_at = current_timestamp where id = ?", cartId);
        return commerceSupport.loadCart(userId);
    }

    @Override
    public CartItemDto.CartResponse updateItem(String email, Long itemId, CartItemDto.UpdateQuantityRequest request) {
        long userId = commerceSupport.requireUserId(email);
        long cartId = commerceSupport.getOrCreateActiveCartId(userId);
        if (itemId == null) {
            throw new CustomException("Cart item not found", HttpStatus.NOT_FOUND);
        }

        Integer quantity = request == null ? null : request.quantity();
        if (quantity == null || quantity < 0) {
            throw new CustomException("Please choose a valid quantity", HttpStatus.BAD_REQUEST);
        }
        if (quantity == 0) {
            return removeItem(email, itemId);
        }

        Long variantId = jdbcTemplate.query("""
                select ci.variant_id
                from cart_items ci
                join carts c on c.id = ci.cart_id
                where ci.id = ? and c.id = ? and lower(c.status) = 'active'
                limit 1
                """, rs -> rs.next() ? rs.getLong("variant_id") : null, itemId, cartId);
        if (variantId == null) {
            throw new CustomException("Cart item not found", HttpStatus.NOT_FOUND);
        }

        String sku = jdbcTemplate.queryForObject("select sku from product_variants where id = ?", String.class, variantId);
        CommerceSupport.VariantRecord variant = commerceSupport.requireVariantBySku(sku);
        if (!variant.canPurchase(quantity)) {
            throw new CustomException("Only " + variant.stockQty() + " unit(s) available for " + variant.title(), HttpStatus.CONFLICT);
        }

        jdbcTemplate.update("""
                update cart_items
                set qty = ?, price_cents_snapshot = ?, currency = ?, updated_at = current_timestamp
                where id = ?
                """, quantity, variant.priceCents(), variant.currency(), itemId);
        jdbcTemplate.update("update carts set updated_at = current_timestamp where id = ?", cartId);
        return commerceSupport.loadCart(userId);
    }

    @Override
    public CartItemDto.CartResponse removeItem(String email, Long itemId) {
        long userId = commerceSupport.requireUserId(email);
        long cartId = commerceSupport.getOrCreateActiveCartId(userId);
        int removed = jdbcTemplate.update("""
                delete ci
                from cart_items ci
                join carts c on c.id = ci.cart_id
                where ci.id = ? and c.id = ? and lower(c.status) = 'active'
                """, itemId, cartId);
        if (removed == 0) {
            throw new CustomException("Cart item not found", HttpStatus.NOT_FOUND);
        }
        jdbcTemplate.update("update carts set updated_at = current_timestamp where id = ?", cartId);
        return commerceSupport.loadCart(userId);
    }

    @Override
    public CartItemDto.CartResponse clearCart(String email) {
        long userId = commerceSupport.requireUserId(email);
        long cartId = commerceSupport.getOrCreateActiveCartId(userId);
        jdbcTemplate.update("delete from cart_items where cart_id = ?", cartId);
        jdbcTemplate.update("update carts set updated_at = current_timestamp where id = ?", cartId);
        return commerceSupport.loadCart(userId);
    }

    private int sanitizeQuantity(Integer quantity) {
        if (quantity == null) {
            return 1;
        }
        if (quantity <= 0) {
            throw new CustomException("Please choose a valid quantity", HttpStatus.BAD_REQUEST);
        }
        return Math.min(quantity, 10);
    }
}
