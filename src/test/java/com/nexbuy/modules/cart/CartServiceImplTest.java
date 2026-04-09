package com.nexbuy.modules.cart;

import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.cart.dto.CartItemDto;
import com.nexbuy.modules.cart.service.impl.CartServiceImpl;
import com.nexbuy.modules.commerce.CommerceSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceImplTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock CommerceSupport commerceSupport;

    @InjectMocks CartServiceImpl cartService;

    private CartItemDto.CartResponse emptyCart() {
        CartItemDto.CartTotals totals = new CartItemDto.CartTotals(0, 0, 0, 0, 0, 0, "INR", null);
        return new CartItemDto.CartResponse(1L, "active", Collections.emptyList(), totals);
    }

    @Nested
    @DisplayName("addItem")
    class AddItemTests {

        @Test
        @DisplayName("Adding item with zero quantity throws BAD_REQUEST")
        void addItem_zeroQty() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            CartItemDto.AddItemRequest req = new CartItemDto.AddItemRequest("SKU-001", 0);

            assertThatThrownBy(() -> cartService.addItem("user@nexbuy.com", req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Adding item with negative quantity throws BAD_REQUEST")
        void addItem_negativeQty() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            CartItemDto.AddItemRequest req = new CartItemDto.AddItemRequest("SKU-001", -3);

            assertThatThrownBy(() -> cartService.addItem("user@nexbuy.com", req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Adding out-of-stock item throws CONFLICT")
        void addItem_outOfStock() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            CommerceSupport.VariantRecord variant = mock(CommerceSupport.VariantRecord.class);
            when(variant.canPurchase(anyInt())).thenReturn(false);
            when(variant.stockQty()).thenReturn(0);
            when(variant.title()).thenReturn("Test Product");
            when(commerceSupport.requireVariantBySku(anyString())).thenReturn(variant);

            CartItemDto.AddItemRequest req = new CartItemDto.AddItemRequest("SKU-001", 1);

            assertThatThrownBy(() -> cartService.addItem("user@nexbuy.com", req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    @Nested
    @DisplayName("updateItem")
    class UpdateItemTests {

        @Test
        @DisplayName("Updating with null quantity throws BAD_REQUEST")
        void updateItem_nullQty() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.getOrCreateActiveCartId(anyLong())).thenReturn(1L);

            CartItemDto.UpdateQuantityRequest req = new CartItemDto.UpdateQuantityRequest(null);

            assertThatThrownBy(() -> cartService.updateItem("user@nexbuy.com", 1L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Updating non-existent item throws NOT_FOUND")
        void updateItem_notFound() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.getOrCreateActiveCartId(anyLong())).thenReturn(1L);
            when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), anyLong(), anyLong()))
                    .thenReturn(null);

            CartItemDto.UpdateQuantityRequest req = new CartItemDto.UpdateQuantityRequest(2);

            assertThatThrownBy(() -> cartService.updateItem("user@nexbuy.com", 99L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItemTests {

        @Test
        @DisplayName("Removing non-existent item throws NOT_FOUND")
        void removeItem_notFound() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.getOrCreateActiveCartId(anyLong())).thenReturn(1L);
            when(jdbcTemplate.update(anyString(), anyLong(), anyLong())).thenReturn(0);

            assertThatThrownBy(() -> cartService.removeItem("user@nexbuy.com", 99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getCart")
    class GetCartTests {

        @Test
        @DisplayName("getCart returns cart for valid user")
        void getCart_success() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.loadCart(1L)).thenReturn(emptyCart());

            CartItemDto.CartResponse result = cartService.getCart("user@nexbuy.com");

            assertThat(result).isNotNull();
            assertThat(result.items()).isEmpty();
        }
    }
}
