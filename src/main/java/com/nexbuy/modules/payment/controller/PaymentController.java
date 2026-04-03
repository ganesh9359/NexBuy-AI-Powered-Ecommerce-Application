package com.nexbuy.modules.payment.controller;

import com.nexbuy.modules.order.dto.OrderDto;
import com.nexbuy.modules.payment.dto.PaymentRequest;
import com.nexbuy.modules.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDto.OrderDetail> getPaymentStatus(Authentication authentication,
                                                                 @PathVariable String orderNumber) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(authentication.getName(), orderNumber));
    }

    @PostMapping("/{orderNumber}/complete")
    public ResponseEntity<PaymentRequest.PaymentResponse> complete(Authentication authentication,
                                                                   @PathVariable String orderNumber,
                                                                   @RequestBody PaymentRequest.CompletePaymentRequest request) {
        return ResponseEntity.ok(paymentService.completePayment(authentication.getName(), orderNumber, request));
    }

    @DeleteMapping("/{orderNumber}")
    public ResponseEntity<Void> cancel(Authentication authentication,
                                       @PathVariable String orderNumber) {
        paymentService.cancelPayment(authentication.getName(), orderNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/callback")
    public ResponseEntity<PaymentRequest.PaymentResponse> callback(@RequestBody PaymentRequest.CallbackRequest request) {
        return ResponseEntity.ok(paymentService.handleCallback(request));
    }
}