package com.nexbuy.modules.order.service;

import com.nexbuy.modules.order.dto.OrderDto;

import java.util.List;

public interface OrderService {
    OrderDto.CheckoutViewResponse getCheckoutView(String email);

    OrderDto.PlaceOrderResponse placeOrder(String email, OrderDto.PlaceOrderRequest request);

    List<OrderDto.OrderSummary> getOrders(String email);

    OrderDto.OrderDetail getOrder(String email, String orderNumber);

    OrderDto.CancelOrderResponse cancelOrder(String email, String orderNumber);

    OrderDto.OrderDetail requestReturn(String email, String orderNumber, OrderDto.ReturnRequest request);
}