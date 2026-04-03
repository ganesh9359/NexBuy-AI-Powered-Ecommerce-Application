package com.nexbuy.modules.order.dto;

import com.nexbuy.modules.cart.dto.CartItemDto;
import com.nexbuy.modules.user.dto.UserProfileDto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    public record CheckoutViewResponse(UserProfileDto.ProfileSummary profile,
                                       List<UserProfileDto.AddressSummary> addresses,
                                       CartItemDto.CartResponse cart,
                                       List<String> availableProviders,
                                       String recommendedProvider) {
    }

    public record PlaceOrderRequest(Long shippingAddressId,
                                    Long billingAddressId,
                                    Boolean billingSameAsShipping,
                                    String paymentProvider) {
    }

    public record ReturnRequest(String reason) {
    }

    public record PaymentIntent(String provider,
                                String status,
                                String providerOrderId,
                                String providerPaymentId,
                                String checkoutLabel,
                                boolean requiresAction,
                                String publicKey) {
    }

    public record RefundSummary(String status,
                                int amountCents,
                                String currency,
                                String providerRefundId,
                                String note,
                                LocalDateTime requestedAt,
                                LocalDateTime processedAt) {
    }

    public record ReturnSummary(String status,
                                String refundStatus,
                                String reason,
                                LocalDateTime requestedAt,
                                LocalDateTime reviewedAt,
                                LocalDateTime updatedAt) {
    }

    public record OrderSummary(String orderNumber,
                               String status,
                               String paymentStatus,
                               int totalCents,
                               String currency,
                               LocalDateTime placedAt,
                               int itemCount) {
    }

    public record OrderLine(String title,
                            String sku,
                            int unitPriceCents,
                            int quantity,
                            int lineTotalCents,
                            String coverImage,
                            String productSlug) {
    }

    public record StatusEvent(String code,
                              String label,
                              LocalDateTime at,
                              String detail) {
    }

    public record OrderDetail(OrderSummary summary,
                              List<OrderLine> items,
                              List<StatusEvent> timeline,
                              String shippingAddress,
                              String billingAddress,
                              PaymentIntent payment,
                              RefundSummary refund,
                              ReturnSummary returnRequest) {
    }

    public record PlaceOrderResponse(OrderDetail order,
                                     CartItemDto.CartResponse cart) {
    }

    public record CancelOrderResponse(boolean removed,
                                      OrderDetail order) {
    }
}