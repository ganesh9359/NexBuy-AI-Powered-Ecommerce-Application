package com.nexbuy.modules.cart.dto;

import java.util.List;

public class CartItemDto {

    public record AddItemRequest(String sku,
                                 Integer quantity) {
    }

    public record UpdateQuantityRequest(Integer quantity) {
    }

    public record CartTotals(int itemCount,
                             int subtotalCents,
                             int taxCents,
                             int shippingCents,
                             int discountCents,
                             int totalCents,
                             String currency) {
    }

    public record CartLine(Long itemId,
                           Long productId,
                           Long variantId,
                           String slug,
                           String title,
                           String coverImage,
                           String categoryName,
                           String brandName,
                           String variantName,
                           String sku,
                           int unitPriceCents,
                           Integer compareAtCents,
                           String currency,
                           int quantity,
                           int stockQty,
                           boolean inStock,
                           boolean backorderAllowed,
                           boolean lowStock,
                           String stockLabel,
                           boolean purchasable,
                           String stockMessage,
                           int lineTotalCents) {
    }

    public record CartResponse(Long cartId,
                               String status,
                               List<CartLine> items,
                               CartTotals totals) {
    }
}