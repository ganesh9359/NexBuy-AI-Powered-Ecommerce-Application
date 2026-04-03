package com.nexbuy.modules.order.service.impl;

import com.nexbuy.exception.CustomException;
import com.nexbuy.integration.email.EmailService;
import com.nexbuy.modules.cart.dto.CartItemDto;
import com.nexbuy.modules.commerce.CommerceSupport;
import com.nexbuy.modules.order.dto.OrderDto;
import com.nexbuy.modules.order.service.OrderService;
import com.nexbuy.modules.payment.dto.PaymentRequest;
import com.nexbuy.modules.payment.integration.PaymentGatewayClient;
import com.nexbuy.modules.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final JdbcTemplate jdbcTemplate;
    private final CommerceSupport commerceSupport;
    private final PaymentGatewayClient paymentGatewayClient;
    private final PaymentService paymentService;
    private final EmailService emailService;

    public OrderServiceImpl(JdbcTemplate jdbcTemplate,
                            CommerceSupport commerceSupport,
                            PaymentGatewayClient paymentGatewayClient,
                            PaymentService paymentService,
                            EmailService emailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.commerceSupport = commerceSupport;
        this.paymentGatewayClient = paymentGatewayClient;
        this.paymentService = paymentService;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.CheckoutViewResponse getCheckoutView(String email) {
        long userId = commerceSupport.requireUserId(email);
        return commerceSupport.buildCheckoutView(userId);
    }

    @Override
    public OrderDto.PlaceOrderResponse placeOrder(String email, OrderDto.PlaceOrderRequest request) {
        long userId = commerceSupport.requireUserId(email);
        CartItemDto.CartResponse cart = commerceSupport.loadCart(userId);
        if (cart.items().isEmpty()) {
            throw new CustomException("Your cart is empty", HttpStatus.BAD_REQUEST);
        }
        commerceSupport.validateCartForCheckout(cart);

        CommerceSupport.AddressRecord shippingAddress = commerceSupport.requireAddress(userId, request == null ? null : request.shippingAddressId());
        boolean billingSameAsShipping = request == null || request.billingSameAsShipping() == null || request.billingSameAsShipping();
        CommerceSupport.AddressRecord billingAddress = billingSameAsShipping
                ? shippingAddress
                : commerceSupport.requireAddress(userId, request.billingAddressId());

        String provider = normalizeProvider(request == null ? null : request.paymentProvider());
        String orderNumber = commerceSupport.generateOrderNumber();
        String shippingSnapshot = commerceSupport.buildAddressSnapshot(shippingAddress);
        String billingSnapshot = commerceSupport.buildAddressSnapshot(billingAddress);

        jdbcTemplate.update("""
                insert into orders (
                    user_id, cart_id, order_number, status, subtotal_cents, tax_cents, shipping_cents,
                    discount_cents, total_cents, currency, payment_status, payment_ref,
                    shipping_address_snapshot, billing_address_snapshot, placed_at, updated_at
                ) values (?, ?, ?, 'pending', ?, ?, ?, ?, ?, ?, 'initiated', null, ?, ?, current_timestamp, current_timestamp)
                """, userId, cart.cartId(), orderNumber, cart.totals().subtotalCents(), cart.totals().taxCents(), cart.totals().shippingCents(),
                cart.totals().discountCents(), cart.totals().totalCents(), cart.totals().currency(), shippingSnapshot, billingSnapshot);
        Long orderId = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        if (orderId == null) {
            throw new CustomException("Could not create the order", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        for (CartItemDto.CartLine item : cart.items()) {
            jdbcTemplate.update("""
                    insert into order_items (
                        order_id, variant_id, title_snapshot, sku_snapshot, attributes_snapshot,
                        unit_price_cents, qty, line_total_cents
                    ) values (?, ?, ?, ?, null, ?, ?, ?)
                    """, orderId, item.variantId(), item.title(), item.sku(), item.unitPriceCents(), item.quantity(), item.lineTotalCents());
        }

        PaymentGatewayClient.GatewayIntent intent = paymentGatewayClient.createIntent(provider, orderNumber, cart.totals().totalCents(), cart.totals().currency());
        jdbcTemplate.update("""
                insert into payments (
                    order_id, provider, amount_cents, currency, status, provider_payment_id, provider_order_id, created_at, updated_at
                ) values (?, ?, ?, ?, 'created', null, ?, current_timestamp, current_timestamp)
                """, orderId, intent.provider(), intent.amountCents(), intent.currency(), intent.providerOrderId());

        OrderDto.OrderDetail detail = commerceSupport.loadOrderDetail(orderNumber);
        if ("cod".equals(provider)) {
            PaymentRequest.PaymentResponse response = paymentService.handleCallback(new PaymentRequest.CallbackRequest(
                    orderNumber,
                    provider,
                    "success",
                    intent.providerOrderId(),
                    null,
                    commerceSupport.generatePaymentReference(provider),
                    null
            ));
            return new OrderDto.PlaceOrderResponse(response.order(), commerceSupport.loadCart(userId));
        }

        return new OrderDto.PlaceOrderResponse(detail, cart);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto.OrderSummary> getOrders(String email) {
        long userId = commerceSupport.requireUserId(email);
        return commerceSupport.loadOrderSummaries(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.OrderDetail getOrder(String email, String orderNumber) {
        long userId = commerceSupport.requireUserId(email);
        return commerceSupport.loadOrderDetailForUser(userId, orderNumber);
    }

    @Override
    public OrderDto.CancelOrderResponse cancelOrder(String email, String orderNumber) {
        long userId = commerceSupport.requireUserId(email);
        CommerceSupport.OrderRecord order = commerceSupport.requireOrderForUser(userId, orderNumber);
        String orderStatus = normalize(order.status());
        if (List.of("shipped", "delivered", "cancelled", "failed").contains(orderStatus)) {
            throw new CustomException("This order can no longer be cancelled", HttpStatus.BAD_REQUEST);
        }

        CommerceSupport.PaymentRecord payment = commerceSupport.loadPayment(order.id());
        boolean settled = isSettled(order, payment);
        boolean refundExpected = false;
        String nextOrderPaymentStatus = settled ? "cancelled" : "cancelled";
        String nextPaymentStatus = "cancelled";
        String refundMailStatus = null;

        if (settled && payment != null) {
            restoreReservedStock(order.id());
            if (isRefundableProvider(payment.provider()) && hasCapturedPayment(payment)) {
                RefundOutcome refundOutcome = startRefund(order, payment);
                refundExpected = refundOutcome.refundExpected();
                nextOrderPaymentStatus = refundOutcome.orderPaymentStatus();
                nextPaymentStatus = refundOutcome.paymentStatus();
                refundMailStatus = refundOutcome.refundMailStatus();
            }
        }

        jdbcTemplate.update("delete from shipments where order_id = ?", order.id());
        if (payment != null) {
            jdbcTemplate.update("update payments set status = ?, updated_at = current_timestamp where id = ?", nextPaymentStatus, payment.id());
        }
        jdbcTemplate.update("""
                update orders
                set status = 'cancelled', payment_status = ?, updated_at = current_timestamp
                where id = ?
                """, nextOrderPaymentStatus, order.id());

        String recipient = loadOrderEmail(order.id());
        emailService.sendOrderCancelled(recipient, order.orderNumber(), refundExpected);
        if (refundMailStatus != null) {
            emailService.sendRefundUpdate(recipient, order.orderNumber(), order.totalCents(), refundMailStatus);
        }

        return new OrderDto.CancelOrderResponse(false, commerceSupport.loadOrderDetail(order.orderNumber()));
    }

    @Override
    public OrderDto.OrderDetail requestReturn(String email, String orderNumber, OrderDto.ReturnRequest request) {
        long userId = commerceSupport.requireUserId(email);
        CommerceSupport.OrderRecord order = commerceSupport.requireOrderForUser(userId, orderNumber);
        if (!"delivered".equals(normalize(order.status()))) {
            throw new CustomException("Returns are available only after delivery", HttpStatus.BAD_REQUEST);
        }
        if (commerceSupport.loadReturnRequest(order.id()) != null) {
            throw new CustomException("A return request already exists for this order", HttpStatus.BAD_REQUEST);
        }

        String reason = normalizeBlank(request == null ? null : request.reason());
        jdbcTemplate.update("""
                insert into return_requests (order_id, status, refund_status, reason, requested_at, updated_at)
                values (?, 'requested', 'not_started', ?, current_timestamp, current_timestamp)
                """, order.id(), reason);

        emailService.sendReturnRequested(loadOrderEmail(order.id()), order.orderNumber());
        return commerceSupport.loadOrderDetail(order.orderNumber());
    }

    private RefundOutcome startRefund(CommerceSupport.OrderRecord order, CommerceSupport.PaymentRecord payment) {
        try {
            PaymentGatewayClient.GatewayRefund gatewayRefund = paymentGatewayClient.refund(
                    payment.provider(),
                    payment.providerPaymentId(),
                    order.totalCents(),
                    order.currency(),
                    order.orderNumber(),
                    "Customer cancelled before shipment"
            );
            String refundStatus = normalize(gatewayRefund.status());
            String persistedStatus = "processed".equals(refundStatus) ? "processed" : "pending";
            jdbcTemplate.update("""
                    insert into refunds (order_id, payment_id, amount_cents, currency, status, provider_refund_id, note, requested_at, processed_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, current_timestamp, ?, current_timestamp)
                    on duplicate key update
                        payment_id = values(payment_id),
                        amount_cents = values(amount_cents),
                        currency = values(currency),
                        status = values(status),
                        provider_refund_id = values(provider_refund_id),
                        note = values(note),
                        processed_at = values(processed_at),
                        updated_at = current_timestamp
                    """,
                    order.id(),
                    payment.id(),
                    gatewayRefund.amountCents(),
                    gatewayRefund.currency(),
                    persistedStatus,
                    gatewayRefund.providerRefundId(),
                    gatewayRefund.note(),
                    "processed".equals(refundStatus) ? java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()) : null
            );
            String paymentStatus = "processed".equals(refundStatus) ? "refunded" : "refund_pending";
            return new RefundOutcome(paymentStatus, paymentStatus, refundStatus, true);
        } catch (CustomException ex) {
            jdbcTemplate.update("""
                    insert into refunds (order_id, payment_id, amount_cents, currency, status, provider_refund_id, note, requested_at, processed_at, updated_at)
                    values (?, ?, ?, ?, 'failed', null, ?, current_timestamp, null, current_timestamp)
                    on duplicate key update
                        payment_id = values(payment_id),
                        amount_cents = values(amount_cents),
                        currency = values(currency),
                        status = 'failed',
                        note = values(note),
                        processed_at = null,
                        updated_at = current_timestamp
                    """,
                    order.id(),
                    payment.id(),
                    order.totalCents(),
                    order.currency(),
                    ex.getMessage()
            );
            return new RefundOutcome("refund_pending", "refund_pending", "failed", true);
        }
    }

    private boolean isSettled(CommerceSupport.OrderRecord order, CommerceSupport.PaymentRecord payment) {
        String orderStatus = normalize(order.status());
        String paymentStatus = normalize(order.paymentStatus());
        String paymentRecordStatus = payment == null ? "" : normalize(payment.status());
        return "paid".equals(orderStatus)
                || List.of("success", "paid", "captured", "authorized", "refunded", "refund_pending").contains(paymentStatus)
                || List.of("captured", "authorized", "refunded", "refund_pending").contains(paymentRecordStatus);
    }

    private boolean hasCapturedPayment(CommerceSupport.PaymentRecord payment) {
        String status = normalize(payment == null ? null : payment.status());
        return List.of("captured", "authorized").contains(status);
    }

    private boolean isRefundableProvider(String provider) {
        String normalized = normalize(provider);
        return List.of("razorpay", "stripe").contains(normalized);
    }

    private void restoreReservedStock(long orderId) {
        jdbcTemplate.query("""
                select oi.variant_id,
                       oi.qty
                from order_items oi
                left join inventory i on i.variant_id = oi.variant_id
                where oi.order_id = ?
                order by oi.id asc
                """, rs -> {
            long variantId = rs.getLong("variant_id");
            int quantity = rs.getInt("qty");

            int updated = jdbcTemplate.update("""
                    update inventory
                    set stock_qty = stock_qty + ?, updated_at = current_timestamp
                    where variant_id = ?
                    """, quantity, variantId);
            if (updated == 0) {
                jdbcTemplate.update("""
                        insert into inventory (variant_id, stock_qty, low_stock_threshold, is_backorder_allowed, updated_at)
                        values (?, ?, 5, false, current_timestamp)
                        """, variantId, quantity);
            }
        }, orderId);
    }

    private String loadOrderEmail(long orderId) {
        String email = jdbcTemplate.queryForObject("""
                select u.email
                from orders o
                join users u on u.id = o.user_id
                where o.id = ?
                limit 1
                """, String.class, orderId);
        if (email == null || email.isBlank()) {
            throw new CustomException("Order email could not be resolved", HttpStatus.NOT_FOUND);
        }
        return email;
    }

    private String normalizeProvider(String provider) {
        String value = provider == null ? "stripe" : provider.trim().toLowerCase(Locale.ROOT);
        if (!List.of("stripe", "razorpay", "cod").contains(value)) {
            throw new CustomException("Unsupported payment provider", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record RefundOutcome(String orderPaymentStatus,
                                 String paymentStatus,
                                 String refundMailStatus,
                                 boolean refundExpected) {
    }
}