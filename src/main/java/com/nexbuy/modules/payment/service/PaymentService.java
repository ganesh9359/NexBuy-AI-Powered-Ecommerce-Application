package com.nexbuy.modules.payment.service;

import com.nexbuy.modules.order.dto.OrderDto;
import com.nexbuy.modules.payment.dto.PaymentRequest;

public interface PaymentService {
    OrderDto.OrderDetail getPaymentStatus(String email, String orderNumber);

    PaymentRequest.PaymentResponse completePayment(String email, String orderNumber, PaymentRequest.CompletePaymentRequest request);

    void cancelPayment(String email, String orderNumber);

    PaymentRequest.PaymentResponse handleCallback(PaymentRequest.CallbackRequest request);
}