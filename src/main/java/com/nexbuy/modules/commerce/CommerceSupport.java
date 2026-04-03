package com.nexbuy.modules.commerce;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.auth.repository.UserRepository;
import com.nexbuy.modules.cart.dto.CartItemDto;
import com.nexbuy.modules.order.dto.OrderDto;
import com.nexbuy.modules.payment.integration.PaymentGatewayClient;
import com.nexbuy.modules.user.dto.UserProfileDto;
import com.nexbuy.modules.user.dto.UserProfileDto.AddressSummary;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class CommerceSupport {

    private static final int SHIPPING_CENTS = 19900;
    private static final int FREE_SHIPPING_THRESHOLD_CENTS = 300000;

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PaymentGatewayClient paymentGatewayClient;

    public CommerceSupport(JdbcTemplate jdbcTemplate,
                           UserRepository userRepository,
                           ObjectMapper objectMapper,
                           PaymentGatewayClient paymentGatewayClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    public long requireUserId(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(user -> user.getId())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }

    public UserProfileDto.ProfileSummary loadProfileSummary(long userId) {
        return jdbcTemplate.queryForObject("""
                select u.id,
                       u.email,
                       coalesce(up.first_name, '') as first_name,
                       coalesce(up.last_name, '') as last_name
                from users u
                left join user_profiles up on up.user_id = u.id
                where u.id = ?
                """, (rs, rowNum) -> new UserProfileDto.ProfileSummary(
                rs.getLong("id"),
                rs.getString("email"),
                blankToNull(rs.getString("first_name")),
                blankToNull(rs.getString("last_name"))
        ), userId);
    }

    public List<UserProfileDto.AddressSummary> loadAddresses(long userId) {
        return jdbcTemplate.query("""
                select id, label, line1, line2, city, state, postal_code, country, is_default
                from addresses
                where user_id = ?
                order by is_default desc, updated_at desc, id desc
                """, (rs, rowNum) -> mapAddress(rs), userId);
    }

    public AddressRecord requireAddress(long userId, Long addressId) {
        if (addressId == null) {
            return loadDefaultAddress(userId)
                    .orElseThrow(() -> new CustomException("Please choose a saved address", HttpStatus.BAD_REQUEST));
        }

        return jdbcTemplate.query("""
                select id, label, line1, line2, city, state, postal_code, country, is_default
                from addresses
                where id = ? and user_id = ?
                """, rs -> rs.next() ? Optional.of(mapAddressRecord(rs)) : Optional.<AddressRecord>empty(), addressId, userId)
                .orElseThrow(() -> new CustomException("Address not found", HttpStatus.NOT_FOUND));
    }

    public Optional<AddressRecord> loadDefaultAddress(long userId) {
        return jdbcTemplate.query("""
                select id, label, line1, line2, city, state, postal_code, country, is_default
                from addresses
                where user_id = ?
                order by is_default desc, updated_at desc, id desc
                limit 1
                """, rs -> rs.next() ? Optional.of(mapAddressRecord(rs)) : Optional.<AddressRecord>empty(), userId);
    }

    @Transactional
    public long getOrCreateActiveCartId(long userId) {
        Optional<Long> existing = jdbcTemplate.query("""
                select id
                from carts
                where user_id = ? and lower(status) = 'active'
                order by id desc
                limit 1
                """, rs -> rs.next() ? Optional.of(rs.getLong("id")) : Optional.<Long>empty(), userId);

        if (existing.isPresent()) {
            return existing.get();
        }

        jdbcTemplate.update("""
                insert into carts (user_id, status, created_at, updated_at)
                values (?, 'active', current_timestamp, current_timestamp)
                """, userId);

        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    public CartItemDto.CartResponse loadCart(long userId) {
        long cartId = getOrCreateActiveCartId(userId);
        List<CartItemDto.CartLine> items = jdbcTemplate.query("""
                select ci.id as item_id,
                       c.id as cart_id,
                       ci.qty,
                       ci.price_cents_snapshot,
                       ci.currency,
                       pv.id as variant_id,
                       pv.sku,
                       pv.name as variant_name,
                       pv.compare_at_cents,
                       p.id as product_id,
                       p.slug,
                       p.title,
                       p.cover_image,
                       cat.name as category_name,
                       b.name as brand_name,
                       coalesce(i.stock_qty, 0) as stock_qty,
                       coalesce(i.low_stock_threshold, 5) as low_stock_threshold,
                       coalesce(i.is_backorder_allowed, false) as is_backorder_allowed
                from carts c
                left join cart_items ci on ci.cart_id = c.id
                left join product_variants pv on pv.id = ci.variant_id
                left join products p on p.id = pv.product_id
                left join categories cat on cat.id = p.category_id
                left join brands b on b.id = p.brand_id
                left join inventory i on i.variant_id = pv.id
                where c.id = ?
                order by ci.updated_at desc, ci.id desc
                """, (rs, rowNum) -> mapCartLine(rs), cartId);

        List<CartItemDto.CartLine> filteredItems = items.stream()
                .filter(item -> item.itemId() != null)
                .toList();

        return new CartItemDto.CartResponse(cartId, "active", filteredItems, calculateTotals(filteredItems));
    }

    public VariantRecord requireVariantBySku(String sku) {
        String normalizedSku = blankToNull(sku);
        if (normalizedSku == null) {
            throw new CustomException("Please choose a valid product variant", HttpStatus.BAD_REQUEST);
        }

        return jdbcTemplate.query("""
                select pv.id as variant_id,
                       pv.product_id,
                       pv.sku,
                       pv.name as variant_name,
                       pv.price_cents,
                       pv.compare_at_cents,
                       pv.currency,
                       p.slug,
                       p.title,
                       p.cover_image,
                       cat.name as category_name,
                       b.name as brand_name,
                       coalesce(i.stock_qty, 0) as stock_qty,
                       coalesce(i.low_stock_threshold, 5) as low_stock_threshold,
                       coalesce(i.is_backorder_allowed, false) as is_backorder_allowed
                from product_variants pv
                join products p on p.id = pv.product_id
                join categories cat on cat.id = p.category_id
                left join brands b on b.id = p.brand_id
                left join inventory i on i.variant_id = pv.id
                where pv.sku = ? and lower(p.status) = 'active'
                limit 1
                """, rs -> rs.next() ? Optional.of(mapVariant(rs)) : Optional.<VariantRecord>empty(), normalizedSku)
                .orElseThrow(() -> new CustomException("Product variant not found", HttpStatus.NOT_FOUND));
    }

    public OrderDto.CheckoutViewResponse buildCheckoutView(long userId) {
        UserProfileDto.ProfileSummary profile = loadProfileSummary(userId);
        List<UserProfileDto.AddressSummary> addresses = loadAddresses(userId);
        CartItemDto.CartResponse cart = loadCart(userId);
        List<String> providers = new ArrayList<>();
        if (paymentGatewayClient != null && paymentGatewayClient.isProviderConfigured("razorpay")) {
            providers.add("razorpay");
        }
        providers.add("cod");
        String recommendedProvider = providers.contains("razorpay") ? "razorpay" : providers.get(0);

        return new OrderDto.CheckoutViewResponse(
                profile,
                addresses,
                cart,
                providers,
                recommendedProvider
        );
    }

    public List<OrderDto.OrderSummary> loadOrderSummaries(long userId) {
        return jdbcTemplate.query("""
                select o.order_number,
                       o.status,
                       o.payment_status,
                       o.total_cents,
                       o.currency,
                       o.placed_at,
                       coalesce(sum(oi.qty), 0) as item_count
                from orders o
                left join order_items oi on oi.order_id = o.id
                where o.user_id = ?
                group by o.id, o.order_number, o.status, o.payment_status, o.total_cents, o.currency, o.placed_at
                order by o.placed_at desc, o.id desc
                """, (rs, rowNum) -> new OrderDto.OrderSummary(
                rs.getString("order_number"),
                rs.getString("status"),
                rs.getString("payment_status"),
                rs.getInt("total_cents"),
                rs.getString("currency"),
                rs.getTimestamp("placed_at").toLocalDateTime(),
                rs.getInt("item_count")
        ), userId);
    }

    public OrderDto.OrderDetail loadOrderDetailForUser(long userId, String orderNumber) {
        OrderRecord order = requireOrderForUser(userId, orderNumber);
        return buildOrderDetail(order);
    }

    public OrderDto.OrderDetail loadOrderDetail(String orderNumber) {
        OrderRecord order = requireOrder(orderNumber);
        return buildOrderDetail(order);
    }

    public OrderRecord requireOrderForUser(long userId, String orderNumber) {
        return jdbcTemplate.query("""
                select id, user_id, cart_id, order_number, status, subtotal_cents, tax_cents, shipping_cents,
                       discount_cents, total_cents, currency, payment_status, payment_ref,
                       shipping_address_snapshot, billing_address_snapshot, placed_at, updated_at
                from orders
                where user_id = ? and order_number = ?
                limit 1
                """, rs -> rs.next() ? Optional.of(mapOrderRecord(rs)) : Optional.<OrderRecord>empty(), userId, orderNumber)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
    }

    public OrderRecord requireOrder(String orderNumber) {
        return jdbcTemplate.query("""
                select id, user_id, cart_id, order_number, status, subtotal_cents, tax_cents, shipping_cents,
                       discount_cents, total_cents, currency, payment_status, payment_ref,
                       shipping_address_snapshot, billing_address_snapshot, placed_at, updated_at
                from orders
                where order_number = ?
                limit 1
                """, rs -> rs.next() ? Optional.of(mapOrderRecord(rs)) : Optional.<OrderRecord>empty(), orderNumber)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
    }

    public PaymentRecord loadPayment(long orderId) {
        return jdbcTemplate.query("""
                select id, provider, amount_cents, currency, status, provider_payment_id, provider_order_id, created_at, updated_at
                from payments
                where order_id = ?
                order by id desc
                limit 1
                """, rs -> rs.next() ? mapPaymentRecord(rs) : null, orderId);
    }

    public RefundRecord loadRefund(long orderId) {
        return jdbcTemplate.query("""
                select amount_cents, currency, status, provider_refund_id, note, requested_at, processed_at, updated_at
                from refunds
                where order_id = ?
                order by id desc
                limit 1
                """, rs -> rs.next() ? mapRefundRecord(rs) : null, orderId);
    }

    public ReturnRequestRecord loadReturnRequest(long orderId) {
        return jdbcTemplate.query("""
                select status, refund_status, reason, requested_at, reviewed_at, updated_at
                from return_requests
                where order_id = ?
                order by id desc
                limit 1
                """, rs -> rs.next() ? mapReturnRequestRecord(rs) : null, orderId);
    }

    public List<OrderDto.OrderLine> loadOrderLines(long orderId) {
        return jdbcTemplate.query("""
                select oi.title_snapshot,
                       oi.sku_snapshot,
                       oi.unit_price_cents,
                       oi.qty,
                       oi.line_total_cents,
                       p.cover_image,
                       p.slug
                from order_items oi
                left join product_variants pv on pv.id = oi.variant_id
                left join products p on p.id = pv.product_id
                where oi.order_id = ?
                order by oi.id asc
                """, (rs, rowNum) -> new OrderDto.OrderLine(
                rs.getString("title_snapshot"),
                rs.getString("sku_snapshot"),
                rs.getInt("unit_price_cents"),
                rs.getInt("qty"),
                rs.getInt("line_total_cents"),
                rs.getString("cover_image"),
                rs.getString("slug")
        ), orderId);
    }

    public List<OrderDto.StatusEvent> buildTimeline(OrderRecord order,
                                                    PaymentRecord payment,
                                                    RefundRecord refund,
                                                    ReturnRequestRecord returnRequest) {
        List<OrderDto.StatusEvent> events = new ArrayList<>();
        String orderStatus = normalizeLower(order.status());
        events.add(new OrderDto.StatusEvent("order_placed", "Order placed", order.placedAt(), "We created your order and saved the address snapshots."));

        if (payment != null) {
            String paymentStatus = normalizeLower(payment.status());
            String paymentLabel = switch (paymentStatus) {
                case "captured", "authorized", "refunded", "refund_pending" -> "Payment successful";
                case "failed" -> "Payment failed";
                case "cancelled" -> "Payment cancelled";
                default -> "Payment initiated";
            };
            String paymentDetail = switch (paymentStatus) {
                case "captured", "authorized", "refunded", "refund_pending" -> "The selected payment provider confirmed the charge.";
                case "failed" -> "The provider reported a payment failure. You can retry from the payment page.";
                case "cancelled" -> "The payment attempt was cancelled before a charge was completed.";
                default -> "Waiting for the provider callback or confirmation.";
            };
            LocalDateTime paymentAt = switch (paymentStatus) {
                case "captured", "authorized", "failed" -> payment.updatedAt();
                default -> payment.createdAt();
            };
            events.add(new OrderDto.StatusEvent("payment_" + paymentStatus, paymentLabel, paymentAt, paymentDetail));
        }

        ShipmentSnapshot shipment = loadShipmentSnapshot(order.id());
        if (shipment != null) {
            String shipmentStatus = alignShipmentStatus(orderStatus, shipment.status());
            if (!shipmentStatus.isBlank()) {
                events.add(buildShipmentEvent(shipmentStatus, shipment.updatedAt(), shipment.shippedAt(), shipment.deliveredAt()));
            }
        } else if ("shipped".equals(orderStatus) || "delivered".equals(orderStatus)) {
            String derivedStatus = "delivered".equals(orderStatus) ? "delivered" : "in_transit";
            events.add(buildShipmentEvent(derivedStatus, order.updatedAt(), order.updatedAt(), "delivered".equals(orderStatus) ? order.updatedAt() : null));
        }

        if (refund != null) {
            events.add(buildRefundEvent(refund));
        }
        if (returnRequest != null) {
            events.add(buildReturnEvent(returnRequest));
            if (refund == null) {
                OrderDto.StatusEvent refundProgressEvent = buildReturnRefundEvent(returnRequest);
                if (refundProgressEvent != null) {
                    events.add(refundProgressEvent);
                }
            }
        }

        if ("failed".equals(orderStatus)) {
            events.add(new OrderDto.StatusEvent("order_failed", "Order failed", order.updatedAt(), "The order is still available for a payment retry."));
        }
        if ("cancelled".equals(orderStatus)) {
            String detail = refund == null
                    ? "The order was cancelled before shipment."
                    : "The order was cancelled and the refund is tracked below.";
            events.add(new OrderDto.StatusEvent("order_cancelled", "Order cancelled", order.updatedAt(), detail));
        }

        return events.stream()
                .sorted(Comparator.comparing(OrderDto.StatusEvent::at, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public String buildAddressSnapshot(AddressRecord address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("label", address.label());
        snapshot.put("line1", address.line1());
        snapshot.put("line2", address.line2());
        snapshot.put("city", address.city());
        snapshot.put("state", address.state());
        snapshot.put("postalCode", address.postalCode());
        snapshot.put("country", address.country());
        snapshot.put("displayText", address.displayText());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (IOException ex) {
            throw new CustomException("Could not prepare address snapshot", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String displayAddress(String snapshotJson) {
        if (blankToNull(snapshotJson) == null) {
            return "";
        }
        try {
            Map<String, Object> map = objectMapper.readValue(snapshotJson, new TypeReference<>() {
            });
            Object displayText = map.get("displayText");
            if (displayText instanceof String value && !value.isBlank()) {
                return value;
            }
            return List.of(
                            Objects.toString(map.get("line1"), ""),
                            Objects.toString(map.get("line2"), ""),
                            joinCityState(map),
                            Objects.toString(map.get("country"), ""))
                    .stream()
                    .filter(part -> part != null && !part.isBlank())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        } catch (IOException ex) {
            return snapshotJson;
        }
    }

    public CartItemDto.CartTotals calculateTotals(List<CartItemDto.CartLine> items) {
        int subtotal = items.stream().mapToInt(CartItemDto.CartLine::lineTotalCents).sum();
        int itemCount = items.stream().mapToInt(CartItemDto.CartLine::quantity).sum();
        int tax = 0;
        int shipping = subtotal == 0 ? 0 : subtotal >= FREE_SHIPPING_THRESHOLD_CENTS ? 0 : SHIPPING_CENTS;
        int discount = 0;
        int total = subtotal + tax + shipping - discount;
        return new CartItemDto.CartTotals(itemCount, subtotal, tax, shipping, discount, total, "INR");
    }

    public void validateCartForCheckout(CartItemDto.CartResponse cart) {
        CartItemDto.CartLine invalid = cart.items().stream()
                .filter(item -> !item.purchasable())
                .findFirst()
                .orElse(null);
        if (invalid == null) {
            return;
        }
        if (invalid.stockQty() <= 0) {
            throw new CustomException(invalid.title() + " is out of stock. Remove it or reduce the quantity to continue.", HttpStatus.CONFLICT);
        }
        throw new CustomException("Only " + invalid.stockQty() + " unit(s) available for " + invalid.title() + ". Reduce the quantity to continue.", HttpStatus.CONFLICT);
    }

    public String generateOrderNumber() {
        return "NBY-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    public String generatePaymentReference(String provider) {
        return provider.toUpperCase(Locale.ROOT) + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
    }

    public OrderDto.OrderDetail buildOrderDetail(OrderRecord order) {
        PaymentRecord payment = loadPayment(order.id());
        RefundRecord refund = loadRefund(order.id());
        ReturnRequestRecord returnRequest = loadReturnRequest(order.id());
        OrderDto.PaymentIntent paymentIntent = payment == null
                ? null
                : new OrderDto.PaymentIntent(
                payment.provider(),
                payment.status(),
                payment.providerOrderId(),
                payment.providerPaymentId(),
                payment.checkoutLabel(),
                payment.requiresAction(),
                paymentGatewayClient == null ? null : paymentGatewayClient.resolvePublicKey(payment.provider())
        );

        OrderDto.RefundSummary refundSummary = refund == null
                ? null
                : new OrderDto.RefundSummary(
                refund.status(),
                refund.amountCents(),
                refund.currency(),
                refund.providerRefundId(),
                refund.note(),
                refund.requestedAt(),
                refund.processedAt()
        );

        OrderDto.ReturnSummary returnSummary = returnRequest == null
                ? null
                : new OrderDto.ReturnSummary(
                returnRequest.status(),
                returnRequest.refundStatus(),
                returnRequest.reason(),
                returnRequest.requestedAt(),
                returnRequest.reviewedAt(),
                returnRequest.updatedAt()
        );

        List<OrderDto.OrderLine> orderLines = loadOrderLines(order.id());
        OrderDto.OrderSummary summary = new OrderDto.OrderSummary(
                order.orderNumber(),
                order.status(),
                order.paymentStatus(),
                order.totalCents(),
                order.currency(),
                order.placedAt(),
                orderLines.stream().mapToInt(OrderDto.OrderLine::quantity).sum()
        );

        return new OrderDto.OrderDetail(
                summary,
                orderLines,
                buildTimeline(order, payment, refund, returnRequest),
                displayAddress(order.shippingAddressSnapshot()),
                displayAddress(order.billingAddressSnapshot()),
                paymentIntent,
                refundSummary,
                returnSummary
        );
    }

    public static String stockLabel(int stockQty, int lowStockThreshold, boolean backorderAllowed) {
        if (stockQty > 0 && stockQty <= lowStockThreshold) {
            return "Low stock";
        }
        if (stockQty > 0) {
            return "In stock";
        }
        return "Out of stock";
    }

    public static String cartStockMessage(int quantity, int stockQty, boolean backorderAllowed) {
        if (stockQty <= 0) {
            return "This item is sold out";
        }
        if (quantity > stockQty) {
            return "Reduce to " + stockQty + " or fewer to continue";
        }
        return stockQty + " unit" + (stockQty == 1 ? "" : "s") + " ready to ship";
    }

    private CartItemDto.CartLine mapCartLine(ResultSet rs) throws SQLException {
        Long itemId = rs.getObject("item_id", Long.class);
        if (itemId == null) {
            return new CartItemDto.CartLine(null, null, null, null, null, null, null, null, null, null,
                    0, null, "INR", 0, 0, false, false, false, "Out of stock", false, "This item is sold out", 0);
        }

        int stockQty = rs.getInt("stock_qty");
        int lowStockThreshold = rs.getInt("low_stock_threshold");
        boolean backorderAllowed = rs.getBoolean("is_backorder_allowed");
        int quantity = rs.getInt("qty");
        boolean purchasable = stockQty >= quantity;
        boolean inStock = stockQty > 0 && purchasable;
        boolean lowStock = stockQty > 0 && stockQty <= lowStockThreshold;
        String cartStockLabel = purchasable ? stockLabel(stockQty, lowStockThreshold, backorderAllowed) : "Out of stock";
        String cartStockMessage = cartStockMessage(quantity, stockQty, backorderAllowed);
        int unitPrice = rs.getInt("price_cents_snapshot");
        return new CartItemDto.CartLine(
                itemId,
                rs.getLong("product_id"),
                rs.getLong("variant_id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("cover_image"),
                rs.getString("category_name"),
                rs.getString("brand_name"),
                rs.getString("variant_name"),
                rs.getString("sku"),
                unitPrice,
                rs.getObject("compare_at_cents", Integer.class),
                rs.getString("currency"),
                quantity,
                stockQty,
                inStock,
                backorderAllowed,
                lowStock,
                cartStockLabel,
                purchasable,
                cartStockMessage,
                unitPrice * quantity
        );
    }

    private UserProfileDto.AddressSummary mapAddress(ResultSet rs) throws SQLException {
        AddressRecord record = mapAddressRecord(rs);
        return new UserProfileDto.AddressSummary(
                record.id(),
                record.label(),
                record.line1(),
                record.line2(),
                record.city(),
                record.state(),
                record.postalCode(),
                record.country(),
                record.isDefault(),
                record.displayText()
        );
    }

    private AddressRecord mapAddressRecord(ResultSet rs) throws SQLException {
        return new AddressRecord(
                rs.getLong("id"),
                blankToNull(rs.getString("label")),
                rs.getString("line1"),
                blankToNull(rs.getString("line2")),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("postal_code"),
                rs.getString("country"),
                rs.getBoolean("is_default")
        );
    }

    private VariantRecord mapVariant(ResultSet rs) throws SQLException {
        int stockQty = rs.getInt("stock_qty");
        int lowStockThreshold = rs.getInt("low_stock_threshold");
        boolean backorderAllowed = rs.getBoolean("is_backorder_allowed");
        return new VariantRecord(
                rs.getLong("variant_id"),
                rs.getLong("product_id"),
                rs.getString("sku"),
                rs.getString("variant_name"),
                rs.getInt("price_cents"),
                rs.getObject("compare_at_cents", Integer.class),
                rs.getString("currency"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("cover_image"),
                rs.getString("category_name"),
                rs.getString("brand_name"),
                stockQty,
                lowStockThreshold,
                backorderAllowed
        );
    }

    private OrderRecord mapOrderRecord(ResultSet rs) throws SQLException {
        return new OrderRecord(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getObject("cart_id", Long.class),
                rs.getString("order_number"),
                rs.getString("status"),
                rs.getInt("subtotal_cents"),
                rs.getInt("tax_cents"),
                rs.getInt("shipping_cents"),
                rs.getInt("discount_cents"),
                rs.getInt("total_cents"),
                rs.getString("currency"),
                rs.getString("payment_status"),
                rs.getString("payment_ref"),
                rs.getString("shipping_address_snapshot"),
                rs.getString("billing_address_snapshot"),
                rs.getTimestamp("placed_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private PaymentRecord mapPaymentRecord(ResultSet rs) throws SQLException {
        String provider = rs.getString("provider");
        String status = rs.getString("status");
        return new PaymentRecord(
                rs.getLong("id"),
                provider,
                rs.getInt("amount_cents"),
                rs.getString("currency"),
                status,
                rs.getString("provider_payment_id"),
                rs.getString("provider_order_id"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                switch (normalizeLower(provider)) {
                    case "cod" -> "Cash on delivery";
                    case "razorpay" -> "Razorpay checkout";
                    default -> "Card payment";
                },
                !"cod".equals(normalizeLower(provider))
                        && !List.of("captured", "authorized", "refunded", "refund_pending", "cancelled").contains(normalizeLower(status))
        );
    }

    private RefundRecord mapRefundRecord(ResultSet rs) throws SQLException {
        return new RefundRecord(
                rs.getInt("amount_cents"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getString("provider_refund_id"),
                rs.getString("note"),
                rs.getTimestamp("requested_at").toLocalDateTime(),
                timestamp(rs, "processed_at", null),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private ReturnRequestRecord mapReturnRequestRecord(ResultSet rs) throws SQLException {
        return new ReturnRequestRecord(
                rs.getString("status"),
                rs.getString("refund_status"),
                rs.getString("reason"),
                rs.getTimestamp("requested_at").toLocalDateTime(),
                timestamp(rs, "reviewed_at", null),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private String joinCityState(Map<String, Object> map) {
        String city = Objects.toString(map.get("city"), "");
        String state = Objects.toString(map.get("state"), "");
        String postal = Objects.toString(map.get("postalCode"), "");
        return List.of(city, state, postal).stream().filter(part -> !part.isBlank()).reduce((left, right) -> left + " " + right).orElse("");
    }

    private int percentage(int amount, int percent) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
                .intValue();
    }

    private ShipmentSnapshot loadShipmentSnapshot(long orderId) {
        return jdbcTemplate.query("""
                select status, shipped_at, delivered_at, updated_at
                from shipments
                where order_id = ?
                order by id desc
                limit 1
                """, rs -> rs.next()
                ? new ShipmentSnapshot(
                normalizeLower(rs.getString("status")),
                timestamp(rs, "shipped_at", null),
                timestamp(rs, "delivered_at", null),
                rs.getTimestamp("updated_at").toLocalDateTime())
                : null, orderId);
    }

    private String alignShipmentStatus(String orderStatus, String shipmentStatus) {
        if ("delivered".equals(orderStatus)) {
            return "delivered";
        }
        if ("shipped".equals(orderStatus) && List.of("pending", "packed").contains(shipmentStatus)) {
            return "in_transit";
        }
        if (List.of("failed", "cancelled").contains(orderStatus) && "pending".equals(shipmentStatus)) {
            return "";
        }
        return shipmentStatus;
    }

    private OrderDto.StatusEvent buildShipmentEvent(String shipmentStatus,
                                                    LocalDateTime updatedAt,
                                                    LocalDateTime shippedAt,
                                                    LocalDateTime deliveredAt) {
        LocalDateTime eventAt = switch (shipmentStatus) {
            case "delivered" -> deliveredAt != null ? deliveredAt : updatedAt;
            case "in_transit" -> shippedAt != null ? shippedAt : updatedAt;
            default -> updatedAt;
        };
        String shipmentLabel = switch (shipmentStatus) {
            case "packed" -> "Shipment packed";
            case "in_transit" -> "In transit";
            case "delivered" -> "Delivered";
            default -> "Shipment pending";
        };
        String shipmentDetail = switch (shipmentStatus) {
            case "packed" -> "The order is packed and waiting for handoff.";
            case "in_transit" -> "Your package is on the way.";
            case "delivered" -> "The shipment was marked as delivered.";
            default -> "The order is waiting in the fulfillment queue.";
        };
        return new OrderDto.StatusEvent("shipment_" + shipmentStatus, shipmentLabel, eventAt, shipmentDetail);
    }

    private OrderDto.StatusEvent buildRefundEvent(RefundRecord refund) {
        String refundStatus = normalizeLower(refund.status());
        LocalDateTime eventAt = switch (refundStatus) {
            case "processed" -> refund.processedAt() != null ? refund.processedAt() : refund.updatedAt();
            default -> refund.requestedAt();
        };
        String label = switch (refundStatus) {
            case "processed" -> "Refund done";
            case "failed" -> "Refund needs attention";
            default -> "Refund initiated";
        };
        String detail = switch (refundStatus) {
            case "processed" -> "The refund was sent back to the original payment method.";
            case "failed" -> refund.note() == null || refund.note().isBlank()
                    ? "The refund could not be completed automatically. Support will help finish it."
                    : refund.note();
            default -> "The payment provider is processing the refund now.";
        };
        return new OrderDto.StatusEvent("refund_" + refundStatus, label, eventAt, detail);
    }

    private OrderDto.StatusEvent buildReturnEvent(ReturnRequestRecord returnRequest) {
        String status = normalizeLower(returnRequest.status());
        LocalDateTime eventAt = "requested".equals(status) ? returnRequest.requestedAt() : returnRequest.updatedAt();
        String label = switch (status) {
            case "approved" -> "Return approved";
            case "rejected" -> "Return rejected";
            case "received" -> "Returned item received";
            case "refunded" -> "Return refunded";
            default -> "Return requested";
        };
        String detail = switch (status) {
            case "approved" -> "The return request was approved and is moving through the return flow.";
            case "rejected" -> "The return request was reviewed and could not be approved.";
            case "received" -> "The returned item was received and is being checked.";
            case "refunded" -> "The return was completed and refund processing has started or finished.";
            default -> "Your return request has been recorded and is waiting for review.";
        };
        return new OrderDto.StatusEvent("return_" + status, label, eventAt, detail);
    }

    private OrderDto.StatusEvent buildReturnRefundEvent(ReturnRequestRecord returnRequest) {
        String refundStatus = normalizeLower(returnRequest.refundStatus());
        if (refundStatus.isBlank() || "not_started".equals(refundStatus)) {
            return null;
        }

        String label = switch (refundStatus) {
            case "processed" -> "Refund done";
            case "failed" -> "Refund needs attention";
            default -> "Refund initiated";
        };
        String detail = switch (refundStatus) {
            case "processed" -> "The refund for this return was sent back to the original payment method.";
            case "failed" -> "The refund for this return needs support attention.";
            default -> "The refund for this return is being processed now.";
        };
        return new OrderDto.StatusEvent("return_refund_" + refundStatus, label, returnRequest.updatedAt(), detail);
    }

    private LocalDateTime timestamp(ResultSet rs, String column, LocalDateTime fallback) throws SQLException {
        return rs.getTimestamp(column) != null ? rs.getTimestamp(column).toLocalDateTime() : fallback;
    }

    private String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record AddressRecord(Long id,
                                String label,
                                String line1,
                                String line2,
                                String city,
                                String state,
                                String postalCode,
                                String country,
                                boolean isDefault) {
        public String displayText() {
            List<String> parts = new ArrayList<>();
            if (line1 != null && !line1.isBlank()) {
                parts.add(line1);
            }
            if (line2 != null && !line2.isBlank()) {
                parts.add(line2);
            }

            StringBuilder locality = new StringBuilder();
            if (city != null && !city.isBlank()) {
                locality.append(city.trim());
            }
            if (state != null && !state.isBlank()) {
                if (locality.length() > 0) {
                    locality.append(", ");
                }
                locality.append(state.trim());
            }
            if (postalCode != null && !postalCode.isBlank()) {
                if (locality.length() > 0) {
                    locality.append(' ');
                }
                locality.append(postalCode.trim());
            }
            if (locality.length() > 0) {
                parts.add(locality.toString());
            }
            if (country != null && !country.isBlank()) {
                parts.add(country);
            }

            return String.join(", ", parts);
        }
    }

    public record VariantRecord(Long variantId,
                                Long productId,
                                String sku,
                                String variantName,
                                int priceCents,
                                Integer compareAtCents,
                                String currency,
                                String slug,
                                String title,
                                String coverImage,
                                String categoryName,
                                String brandName,
                                int stockQty,
                                int lowStockThreshold,
                                boolean backorderAllowed) {
        public boolean canPurchase(int quantity) {
            return stockQty >= quantity;
        }
    }

    public record OrderRecord(Long id,
                              Long userId,
                              Long cartId,
                              String orderNumber,
                              String status,
                              int subtotalCents,
                              int taxCents,
                              int shippingCents,
                              int discountCents,
                              int totalCents,
                              String currency,
                              String paymentStatus,
                              String paymentRef,
                              String shippingAddressSnapshot,
                              String billingAddressSnapshot,
                              LocalDateTime placedAt,
                              LocalDateTime updatedAt) {
    }

    public record PaymentRecord(Long id,
                                String provider,
                                int amountCents,
                                String currency,
                                String status,
                                String providerPaymentId,
                                String providerOrderId,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt,
                                String checkoutLabel,
                                boolean requiresAction) {
    }

    public record RefundRecord(int amountCents,
                               String currency,
                               String status,
                               String providerRefundId,
                               String note,
                               LocalDateTime requestedAt,
                               LocalDateTime processedAt,
                               LocalDateTime updatedAt) {
    }

    public record ReturnRequestRecord(String status,
                                      String refundStatus,
                                      String reason,
                                      LocalDateTime requestedAt,
                                      LocalDateTime reviewedAt,
                                      LocalDateTime updatedAt) {
    }

    private record ShipmentSnapshot(String status,
                                    LocalDateTime shippedAt,
                                    LocalDateTime deliveredAt,
                                    LocalDateTime updatedAt) {
    }
}
