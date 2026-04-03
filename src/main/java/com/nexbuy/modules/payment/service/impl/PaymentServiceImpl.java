package com.nexbuy.modules.payment.service.impl;

import com.nexbuy.exception.CustomException;
import com.nexbuy.integration.email.EmailService;
import com.nexbuy.modules.commerce.CommerceSupport;
import com.nexbuy.modules.order.dto.OrderDto;
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
public class PaymentServiceImpl implements PaymentService {

    private final JdbcTemplate jdbcTemplate;
    private final CommerceSupport commerceSupport;
    private final EmailService emailService;
    private final PaymentGatewayClient paymentGatewayClient;

    public PaymentServiceImpl(JdbcTemplate jdbcTemplate,
                              CommerceSupport commerceSupport,
                              EmailService emailService,
                              PaymentGatewayClient paymentGatewayClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.commerceSupport = commerceSupport;
        this.emailService = emailService;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.OrderDetail getPaymentStatus(String email, String orderNumber) {
        long userId = commerceSupport.requireUserId(email);
        return commerceSupport.loadOrderDetailForUser(userId, orderNumber);
    }

    @Override
    public PaymentRequest.PaymentResponse completePayment(String email, String orderNumber, PaymentRequest.CompletePaymentRequest request) {
        long userId = commerceSupport.requireUserId(email);
        CommerceSupport.OrderRecord order = commerceSupport.requireOrderForUser(userId, orderNumber);
        CommerceSupport.PaymentRecord payment = commerceSupport.loadPayment(order.id());
        if (payment == null) {
            throw new CustomException("Payment record not found", HttpStatus.NOT_FOUND);
        }

        String provider = normalize(payment.provider());
        String outcome = normalize(request == null ? null : request.outcome());
        if (outcome.isBlank()) {
            outcome = "success";
        }

        if ("razorpay".equals(provider) && !List.of("failed", "failure").contains(outcome)) {
            PaymentGatewayClient.RazorpayVerification verification = paymentGatewayClient.verifyRazorpayPayment(
                    payment.providerOrderId(),
                    request == null ? null : request.providerOrderId(),
                    request == null ? null : request.providerPaymentId(),
                    request == null ? null : request.signature(),
                    order.totalCents(),
                    order.currency()
            );
            return applyOutcome(new PaymentRequest.CallbackRequest(
                    orderNumber,
                    provider,
                    verification.status(),
                    verification.providerOrderId(),
                    verification.providerPaymentId(),
                    commerceSupport.generatePaymentReference(provider),
                    request == null ? null : request.signature()
            ));
        }

        return applyOutcome(new PaymentRequest.CallbackRequest(
                orderNumber,
                provider,
                outcome,
                request == null ? null : request.providerOrderId(),
                request == null ? null : request.providerPaymentId(),
                null,
                request == null ? null : request.signature()
        ));
    }

    @Override
    public void cancelPayment(String email, String orderNumber) {
        long userId = commerceSupport.requireUserId(email);
        CommerceSupport.OrderRecord order = commerceSupport.requireOrderForUser(userId, orderNumber);

        String orderStatus = normalize(order.status());
        String paymentStatus = normalize(order.paymentStatus());
        if (List.of("success", "paid", "captured", "authorized").contains(paymentStatus)
                || List.of("paid", "shipped", "delivered", "cancelled").contains(orderStatus)) {
            throw new CustomException("This order can no longer be cancelled from payment", HttpStatus.BAD_REQUEST);
        }

        jdbcTemplate.update("delete from shipments where order_id = ?", order.id());
        jdbcTemplate.update("delete from payments where order_id = ?", order.id());
        jdbcTemplate.update("delete from order_items where order_id = ?", order.id());
        int deleted = jdbcTemplate.update("delete from orders where id = ?", order.id());
        if (deleted == 0) {
            throw new CustomException("Order not found", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public PaymentRequest.PaymentResponse handleCallback(PaymentRequest.CallbackRequest request) {
        return applyOutcome(request);
    }

    private PaymentRequest.PaymentResponse applyOutcome(PaymentRequest.CallbackRequest request) {
        if (request == null || request.orderNumber() == null || request.status() == null) {
            throw new CustomException("Payment callback is incomplete", HttpStatus.BAD_REQUEST);
        }

        CommerceSupport.OrderRecord order = commerceSupport.requireOrder(request.orderNumber());
        CommerceSupport.PaymentRecord payment = commerceSupport.loadPayment(order.id());
        if (payment == null) {
            throw new CustomException("Payment record not found", HttpStatus.NOT_FOUND);
        }
        if ("success".equalsIgnoreCase(order.paymentStatus())) {
            return new PaymentRequest.PaymentResponse(commerceSupport.loadOrderDetail(order.orderNumber()));
        }

        String outcome = normalize(request.status());
        boolean success = List.of("success", "captured", "authorized").contains(outcome);
        boolean failed = List.of("failed", "failure").contains(outcome);
        if (!success && !failed) {
            throw new CustomException("Unsupported payment outcome", HttpStatus.BAD_REQUEST);
        }

        if (success) {
            String storedPaymentStatus = "authorized".equals(outcome) ? "authorized" : "captured";
            reserveAndDeductStock(order.id());
            jdbcTemplate.update("""
                    update payments
                    set status = ?,
                        provider_payment_id = coalesce(?, provider_payment_id),
                        provider_order_id = coalesce(?, provider_order_id),
                        updated_at = current_timestamp
                    where order_id = ?
                    """, storedPaymentStatus, request.providerPaymentId(), request.providerOrderId(), order.id());
            jdbcTemplate.update("""
                    update orders
                    set status = 'paid', payment_status = 'success', payment_ref = ?, updated_at = current_timestamp
                    where id = ?
                    """, request.paymentRef() == null ? commerceSupport.generatePaymentReference(payment.provider()) : request.paymentRef(), order.id());
            if (order.cartId() != null) {
                jdbcTemplate.update("update carts set status = 'converted', updated_at = current_timestamp where id = ?", order.cartId());
            }
            ensureShipment(order.id());
            sendReceipt(order.id(), order.orderNumber(), order.totalCents());
            return new PaymentRequest.PaymentResponse(commerceSupport.loadOrderDetail(order.orderNumber()));
        }

        jdbcTemplate.update("""
                update payments
                set status = 'failed',
                    provider_payment_id = coalesce(?, provider_payment_id),
                    provider_order_id = coalesce(?, provider_order_id),
                    updated_at = current_timestamp
                where order_id = ?
                """, request.providerPaymentId(), request.providerOrderId(), order.id());
        jdbcTemplate.update("""
                update orders
                set status = 'failed', payment_status = 'failed', payment_ref = ?, updated_at = current_timestamp
                where id = ?
                """, request.paymentRef() == null ? commerceSupport.generatePaymentReference(payment.provider()) : request.paymentRef(), order.id());
        return new PaymentRequest.PaymentResponse(commerceSupport.loadOrderDetail(order.orderNumber()));
    }

    private void reserveAndDeductStock(long orderId) {
        jdbcTemplate.query("""
                select oi.variant_id, oi.qty, oi.title_snapshot,
                       coalesce(i.stock_qty, 0) as stock_qty,
                       coalesce(i.is_backorder_allowed, false) as is_backorder_allowed
                from order_items oi
                left join inventory i on i.variant_id = oi.variant_id
                where oi.order_id = ?
                order by oi.id asc
                """, rs -> {
            long variantId = rs.getLong("variant_id");
            int quantity = rs.getInt("qty");
            int stockQty = rs.getInt("stock_qty");
            String title = rs.getString("title_snapshot");

            if (stockQty < quantity) {
                throw new CustomException("Not enough stock left for " + title, HttpStatus.CONFLICT);
            }

            int updated = jdbcTemplate.update("""
                    update inventory
                    set stock_qty = stock_qty - ?, updated_at = current_timestamp
                    where variant_id = ? and stock_qty >= ?
                    """, quantity, variantId, quantity);
            if (updated == 0) {
                throw new CustomException("Not enough stock left for " + title, HttpStatus.CONFLICT);
            }
        }, orderId);
    }

    private void ensureShipment(long orderId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from shipments where order_id = ?", Integer.class, orderId);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                insert into shipments (order_id, status, updated_at)
                values (?, 'pending', current_timestamp)
                """, orderId);
    }

    private void sendReceipt(long orderId, String orderNumber, int totalCents) {
        String email = jdbcTemplate.queryForObject("""
                select u.email
                from orders o
                join users u on u.id = o.user_id
                where o.id = ?
                """, String.class, orderId);

        List<String> items = jdbcTemplate.query("""
                select title_snapshot, qty, line_total_cents
                from order_items
                where order_id = ?
                order by id asc
                """, (rs, rowNum) -> rs.getString("title_snapshot") + " x" + rs.getInt("qty") + " - INR " + String.format("%.2f", rs.getInt("line_total_cents") / 100.0), orderId);
        emailService.sendOrderReceipt(email, orderNumber, totalCents, items);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}