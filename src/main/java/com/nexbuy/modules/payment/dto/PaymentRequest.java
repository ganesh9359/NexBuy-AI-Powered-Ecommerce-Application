package com.nexbuy.modules.payment.dto;

import com.nexbuy.modules.order.dto.OrderDto;

public class PaymentRequest {

    public record CompletePaymentRequest(String outcome,
                                         String providerOrderId,
                                         String providerPaymentId,
                                         String signature) {
    }

    public record CallbackRequest(String orderNumber,
                                  String provider,
                                  String status,
                                  String providerOrderId,
                                  String providerPaymentId,
                                  String paymentRef,
                                  String signature) {
    }

    public record PaymentResponse(OrderDto.OrderDetail order) {
    }
}