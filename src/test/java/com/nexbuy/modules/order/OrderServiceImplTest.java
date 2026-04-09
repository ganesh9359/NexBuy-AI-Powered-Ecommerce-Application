package com.nexbuy.modules.order;

import com.nexbuy.exception.CustomException;
import com.nexbuy.integration.email.EmailService;
import com.nexbuy.modules.cart.dto.CartItemDto;
import com.nexbuy.modules.commerce.CommerceSupport;
import com.nexbuy.modules.order.service.impl.OrderServiceImpl;
import com.nexbuy.modules.payment.integration.PaymentGatewayClient;
import com.nexbuy.modules.payment.service.PaymentService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceImplTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock CommerceSupport commerceSupport;
    @Mock PaymentGatewayClient paymentGatewayClient;
    @Mock PaymentService paymentService;
    @Mock EmailService emailService;

    @InjectMocks OrderServiceImpl orderService;

    // ── getOrder ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrder")
    class GetOrderTests {

        @Test
        @DisplayName("Unknown order number throws NOT_FOUND")
        void getOrder_notFound() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.loadOrderDetailForUser(anyLong(), anyString()))
                    .thenThrow(new CustomException("Order not found", HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> orderService.getOrder("user@nexbuy.com", "NB-GHOST"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("Cancelling delivered order throws BAD_REQUEST")
        void cancelOrder_delivered() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.requireOrderForUser(anyLong(), anyString()))
                    .thenThrow(new CustomException("This order can no longer be cancelled", HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> orderService.cancelOrder("user@nexbuy.com", "NB-001"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Cancelling already cancelled order throws BAD_REQUEST")
        void cancelOrder_alreadyCancelled() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.requireOrderForUser(anyLong(), anyString()))
                    .thenThrow(new CustomException("This order can no longer be cancelled", HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> orderService.cancelOrder("user@nexbuy.com", "NB-001"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Cancelling shipped order throws BAD_REQUEST")
        void cancelOrder_shipped() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.requireOrderForUser(anyLong(), anyString()))
                    .thenThrow(new CustomException("This order can no longer be cancelled", HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> orderService.cancelOrder("user@nexbuy.com", "NB-001"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Unknown order throws NOT_FOUND")
        void cancelOrder_notFound() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.requireOrderForUser(anyLong(), anyString()))
                    .thenThrow(new CustomException("Order not found", HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> orderService.cancelOrder("user@nexbuy.com", "NB-GHOST"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ── requestReturn ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("requestReturn")
    class RequestReturnTests {

        @Test
        @DisplayName("Return on non-delivered order throws BAD_REQUEST")
        void requestReturn_notDelivered() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.requireOrderForUser(anyLong(), anyString()))
                    .thenThrow(new CustomException("Returns are available only after delivery", HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> orderService.requestReturn("user@nexbuy.com", "NB-001", null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Unknown order throws NOT_FOUND")
        void requestReturn_notFound() {
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.requireOrderForUser(anyLong(), anyString()))
                    .thenThrow(new CustomException("Order not found", HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> orderService.requestReturn("user@nexbuy.com", "NB-GHOST", null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrderTests {

        @Test
        @DisplayName("Placing order with empty cart throws BAD_REQUEST")
        void placeOrder_emptyCart() {
            CartItemDto.CartTotals totals = new CartItemDto.CartTotals(0, 0, 0, 0, 0, 0, "INR", null);
            CartItemDto.CartResponse emptyCart = new CartItemDto.CartResponse(1L, "active", Collections.emptyList(), totals);
            when(commerceSupport.requireUserId(anyString())).thenReturn(1L);
            when(commerceSupport.loadCart(anyLong())).thenReturn(emptyCart);

            assertThatThrownBy(() -> orderService.placeOrder("user@nexbuy.com", null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }
}
