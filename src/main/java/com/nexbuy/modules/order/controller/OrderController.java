package com.nexbuy.modules.order.controller;

import com.nexbuy.modules.order.dto.OrderDto;
import com.nexbuy.modules.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public ResponseEntity<OrderDto.CheckoutViewResponse> getCheckout(Authentication authentication) {
        return ResponseEntity.ok(orderService.getCheckoutView(authentication.getName()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderDto.PlaceOrderResponse> placeOrder(Authentication authentication,
                                                                  @RequestBody OrderDto.PlaceOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(authentication.getName(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderDto.OrderSummary>> getOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrders(authentication.getName()));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDto.OrderDetail> getOrder(Authentication authentication,
                                                         @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrder(authentication.getName(), orderNumber));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderDto.CancelOrderResponse> cancelOrder(Authentication authentication,
                                                                    @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.cancelOrder(authentication.getName(), orderNumber));
    }

    @PostMapping("/{orderNumber}/return")
    public ResponseEntity<OrderDto.OrderDetail> requestReturn(Authentication authentication,
                                                              @PathVariable String orderNumber,
                                                              @RequestBody(required = false) OrderDto.ReturnRequest request) {
        return ResponseEntity.ok(orderService.requestReturn(authentication.getName(), orderNumber, request));
    }
}