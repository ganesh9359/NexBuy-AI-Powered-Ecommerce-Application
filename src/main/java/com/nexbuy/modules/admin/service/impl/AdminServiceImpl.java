package com.nexbuy.modules.admin.service.impl;

import com.nexbuy.enums.Role;
import com.nexbuy.enums.UserStatus;
import com.nexbuy.exception.CustomException;
import com.nexbuy.integration.email.EmailService;
import com.nexbuy.modules.admin.dto.AdminBrandDto;
import com.nexbuy.modules.admin.dto.AdminBrandRequest;
import com.nexbuy.modules.admin.dto.AdminCreateAdminRequest;
import com.nexbuy.modules.admin.dto.AdminDashboardDto;
import com.nexbuy.modules.admin.dto.AdminOrderDto;
import com.nexbuy.modules.admin.dto.AdminOrderStatusRequest;
import com.nexbuy.modules.admin.dto.AdminProductDto;
import com.nexbuy.modules.admin.dto.AdminProductMediaDto;
import com.nexbuy.modules.admin.dto.AdminProductRequest;
import com.nexbuy.modules.admin.dto.AdminProductStockRequest;
import com.nexbuy.modules.admin.dto.AdminReturnReviewRequest;
import com.nexbuy.modules.admin.dto.AdminReturnUpdateRequest;
import com.nexbuy.modules.admin.dto.AdminUserDto;
import com.nexbuy.modules.admin.dto.RefundStatusDto;
import com.nexbuy.modules.admin.dto.ReturnRequestDto;
import com.nexbuy.modules.admin.service.AdminService;
import com.nexbuy.modules.auth.entity.User;
import com.nexbuy.modules.auth.repository.UserRepository;
import com.nexbuy.modules.commerce.CommerceSupport;
import com.nexbuy.modules.order.dto.OrderDto;
import com.nexbuy.modules.payment.integration.PaymentGatewayClient;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of("pending", "paid", "failed", "shipped", "delivered", "cancelled");
    private static final Set<String> ALLOWED_PRODUCT_STATUSES = Set.of("active", "inactive");

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommerceSupport commerceSupport;
    private final EmailService emailService;
    private final PaymentGatewayClient paymentGatewayClient;

    private final RowMapper<AdminUserDto> userRowMapper = (rs, rowNum) -> {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(rs.getLong("id"));
        dto.setEmail(rs.getString("email"));
        dto.setPhone(rs.getString("phone"));
        dto.setRole(normalizeLabel(rs.getString("role")));
        dto.setStatus(normalizeLabel(rs.getString("status")));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        dto.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        dto.setLastLoginAt(lastLogin != null ? lastLogin.toLocalDateTime() : null);
        return dto;
    };

    private final RowMapper<AdminOrderDto> orderRowMapper = (rs, rowNum) -> {
        AdminOrderDto dto = new AdminOrderDto();
        dto.setId(rs.getLong("id"));
        dto.setOrderNumber(rs.getString("order_number"));
        dto.setUserEmail(rs.getString("user_email"));
        dto.setStatus(normalizeLabel(rs.getString("status")));
        dto.setTotalCents(rs.getInt("total_cents"));
        dto.setPaymentStatus(normalizeLabel(rs.getString("payment_status")));
        Timestamp placedAt = rs.getTimestamp("placed_at");
        dto.setPlacedAt(placedAt != null ? placedAt.toLocalDateTime() : null);
        return dto;
    };

    private final RowMapper<AdminBrandDto> brandRowMapper = (rs, rowNum) -> {
        AdminBrandDto dto = new AdminBrandDto();
        dto.setId(rs.getLong("id"));
        dto.setName(rs.getString("name"));
        dto.setSlug(rs.getString("slug"));
        return dto;
    };

    private final RowMapper<AdminProductDto> productRowMapper = (rs, rowNum) -> {
        AdminProductDto dto = new AdminProductDto();
        dto.setId(rs.getLong("id"));
        dto.setTitle(rs.getString("title"));
        dto.setSlug(rs.getString("slug"));
        dto.setDescription(rs.getString("description"));
        dto.setCoverImage(rs.getString("cover_image"));
        dto.setStatus(normalizeLabel(rs.getString("status")));
        dto.setCategoryName(rs.getString("category_name"));
        dto.setBrandName(rs.getString("brand_name"));
        dto.setSku(rs.getString("sku"));
        dto.setVariantName(rs.getString("variant_name"));
        dto.setPriceCents(rs.getInt("price_cents"));
        dto.setCompareAtCents(rs.getObject("compare_at_cents", Integer.class));
        dto.setStockQty(rs.getInt("stock_qty"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        dto.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        return dto;
    };

    private final RowMapper<AdminProductMediaDto> productMediaRowMapper = (rs, rowNum) -> {
        AdminProductMediaDto dto = new AdminProductMediaDto();
        dto.setId(rs.getLong("id"));
        dto.setUrl(rs.getString("url"));
        dto.setAltText(rs.getString("alt_text"));
        dto.setSortOrder(rs.getInt("sort_order"));
        return dto;
    };

    public AdminServiceImpl(JdbcTemplate jdbcTemplate,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            CommerceSupport commerceSupport,
                            EmailService emailService,
                            PaymentGatewayClient paymentGatewayClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.commerceSupport = commerceSupport;
        this.emailService = emailService;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    @Override
    public AdminDashboardDto getDashboard() {
        AdminDashboardDto dto = new AdminDashboardDto();
        dto.setTotalUsers(queryCount("select count(*) from users"));
        dto.setTotalProducts(queryCount("select count(*) from products"));
        dto.setTotalOrders(queryCount("select count(*) from orders"));
        dto.setNewOrders(queryCount("select count(*) from orders where lower(status) = 'pending'"));
        dto.setDeliveredOrders(queryCount("select count(*) from orders where lower(status) = 'delivered'"));

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        Arrays.asList("pending", "paid", "shipped", "delivered", "cancelled", "failed")
                .forEach(status -> statusCounts.put(status, 0L));

        jdbcTemplate.query("select lower(status) as status, count(*) as total from orders group by lower(status)", rs -> {
            statusCounts.put(rs.getString("status"), rs.getLong("total"));
        });
        dto.setOrderStatusCounts(statusCounts);
        return dto;
    }

    @Override
    public List<AdminUserDto> getUsers() {
        return jdbcTemplate.query("""
                select id, email, phone, role, status, created_at, last_login_at
                from users
                order by created_at desc
                """, userRowMapper);
    }

    @Override
    public List<AdminUserDto> getAdmins() {
        return jdbcTemplate.query("""
                select id, email, phone, role, status, created_at, last_login_at
                from users
                where upper(role) = 'ADMIN'
                order by created_at desc
                """, userRowMapper);
    }

    @Override
    @Transactional
    public AdminUserDto createAdmin(AdminCreateAdminRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByEmail(email)) {
            throw new CustomException("Email already registered", HttpStatus.CONFLICT);
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new CustomException("Phone number already registered", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        return mapSavedUser(saved);
    }

    @Override
    public List<AdminBrandDto> getBrands() {
        return jdbcTemplate.query("""
                select id, name, slug
                from brands
                order by name asc
                """, brandRowMapper);
    }

    @Override
    @Transactional
    public AdminBrandDto createBrand(AdminBrandRequest request) {
        String name = request.getName().trim();
        if (findBrandId(name).isPresent()) {
            throw new CustomException("Brand already exists", HttpStatus.CONFLICT);
        }

        String slug = uniqueBrandSlug(slugify(name));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into brands (name, slug, created_at, updated_at) values (?, ?, current_timestamp, current_timestamp)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, slug);
            return ps;
        }, keyHolder);

        AdminBrandDto dto = new AdminBrandDto();
        dto.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        dto.setName(name);
        dto.setSlug(slug);
        return dto;
    }

    @Override
    public List<AdminOrderDto> getOrders() {
        return jdbcTemplate.query("""
                select o.id, o.order_number, o.status, o.total_cents, o.payment_status, o.placed_at,
                       u.email as user_email
                from orders o
                left join users u on u.id = o.user_id
                order by o.placed_at desc
                """, orderRowMapper);
    }

    @Override
    public OrderDto.OrderDetail getOrderDetail(Long orderId) {
        AdminOrderDto order = getOrderById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
        return commerceSupport.loadOrderDetail(order.getOrderNumber());
    }

    @Override
    @Transactional
    public AdminOrderDto updateOrderStatus(Long orderId, AdminOrderStatusRequest request) {
        AdminOrderDto existing = getOrderById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
        String status = normalizeOrderStatus(request.getStatus());
        if ("DELIVERED".equals(existing.getStatus()) || "CANCELLED".equals(existing.getStatus())) {
            if (existing.getStatus().equalsIgnoreCase(status)) {
                return existing;
            }
            throw new CustomException("Delivered or cancelled orders can no longer be edited", HttpStatus.BAD_REQUEST);
        }
        int updated = jdbcTemplate.update("update orders set status = ?, updated_at = current_timestamp where id = ?", status, orderId);
        if (updated == 0) {
            throw new CustomException("Order not found", HttpStatus.NOT_FOUND);
        }
        syncShipmentStatus(orderId, status);
        if ("delivered".equals(status)) {
            sendDeliveredEmail(orderId);
        }
        return getOrderById(orderId).orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public OrderDto.OrderDetail reviewReturn(Long orderId, AdminReturnReviewRequest request) {
        AdminOrderDto existing = getOrderById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
        CommerceSupport.OrderRecord order = commerceSupport.requireOrder(existing.getOrderNumber());
        CommerceSupport.ReturnRequestRecord returnRequest = commerceSupport.loadReturnRequest(order.id());
        if (returnRequest == null) {
            throw new CustomException("No return request was found for this order", HttpStatus.BAD_REQUEST);
        }
        if (!"requested".equals(normalizeLower(returnRequest.status()))) {
            throw new CustomException("This return request has already been reviewed", HttpStatus.BAD_REQUEST);
        }

        String action = normalizeLower(request == null ? null : request.getAction());
        if (!List.of("approve", "approved", "reject", "rejected").contains(action)) {
            throw new CustomException("Return review action must be approve or reject", HttpStatus.BAD_REQUEST);
        }

        if (action.startsWith("reject")) {
            jdbcTemplate.update("""
                    update return_requests
                    set status = 'rejected',
                        refund_status = 'not_started',
                        reviewed_at = current_timestamp,
                        updated_at = current_timestamp
                    where order_id = ?
                    """, order.id());
            return commerceSupport.loadOrderDetail(order.orderNumber());
        }

        CommerceSupport.PaymentRecord payment = commerceSupport.loadPayment(order.id());
        String nextReturnStatus = "approved";
        String nextRefundStatus = "not_started";
        String nextOrderPaymentStatus = order.paymentStatus();
        String nextPaymentStatus = payment == null ? null : payment.status();
        String refundMailStatus = null;

        if (payment != null && isRefundableProvider(payment.provider()) && hasCapturedPayment(payment)) {
            RefundOutcome refundOutcome = startRefund(order, payment, "Admin approved the return");
            nextReturnStatus = refundOutcome.returnStatus();
            nextRefundStatus = refundOutcome.refundStatus();
            nextOrderPaymentStatus = refundOutcome.orderPaymentStatus();
            nextPaymentStatus = refundOutcome.paymentStatus();
            refundMailStatus = refundOutcome.refundMailStatus();
        }

        jdbcTemplate.update("""
                update return_requests
                set status = ?,
                    refund_status = ?,
                    reviewed_at = current_timestamp,
                    updated_at = current_timestamp
                where order_id = ?
                """, nextReturnStatus, nextRefundStatus, order.id());

        if (payment != null && nextPaymentStatus != null) {
            jdbcTemplate.update("update payments set status = ?, updated_at = current_timestamp where id = ?", nextPaymentStatus, payment.id());
        }
        if (nextOrderPaymentStatus != null && !nextOrderPaymentStatus.isBlank()) {
            jdbcTemplate.update("update orders set payment_status = ?, updated_at = current_timestamp where id = ?", nextOrderPaymentStatus, order.id());
        }

        restoreStock(order.id());
        if (refundMailStatus != null) {
            emailService.sendRefundUpdate(loadOrderEmail(order.id()), order.orderNumber(), order.totalCents(), refundMailStatus);
        }

        return commerceSupport.loadOrderDetail(order.orderNumber());
    }

    @Override
    public List<AdminProductDto> getProducts() {
        List<AdminProductDto> products = jdbcTemplate.query(productBaseQuery() + " order by p.created_at desc", productRowMapper);
        hydrateProductRelationships(products);
        return products;
    }

    @Override
    public AdminProductDto getProduct(Long productId) {
        AdminProductDto product = getProductById(productId).orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
        hydrateProductRelationships(List.of(product));
        return product;
    }
    @Override
    @Transactional
    public AdminProductDto createProduct(AdminProductRequest request) {
        long categoryId = ensureCategory(defaultIfBlank(request.getCategoryName(), "General"));
        Long brandId = ensureBrand(request.getBrandName());
        String slug = uniqueProductSlug(defaultIfBlank(request.getSlug(), slugify(request.getTitle())), null);
        String sku = uniqueSku(defaultIfBlank(request.getSku(), generateSku(request.getTitle())), null);
        String variantName = defaultIfBlank(request.getVariantName(), request.getTitle());
        String status = normalizeProductStatus(request.getStatus());
        List<String> normalizedTags = normalizeTags(request.getTags());
        String coverImage = resolveCoverImage(request.getCoverImage(), request.getMedia());
        List<AdminProductMediaDto> normalizedMedia = normalizeMedia(request.getMedia(), coverImage, request.getTitle());
        final String resolvedCoverImage = coverImage;

        KeyHolder productKey = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into products (category_id, brand_id, title, slug, description, cover_image, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, categoryId);
            if (brandId == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, brandId);
            }
            ps.setString(3, request.getTitle().trim());
            ps.setString(4, slug);
            ps.setString(5, emptyToNull(request.getDescription()));
            ps.setString(6, emptyToNull(resolvedCoverImage));
            ps.setString(7, status);
            return ps;
        }, productKey);

        long productId = Objects.requireNonNull(productKey.getKey()).longValue();

        KeyHolder variantKey = new GeneratedKeyHolder();
        Integer compareAtCents = normalizeCompareAtCents(request.getCompareAtCents(), request.getPriceCents());
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into product_variants (product_id, sku, name, price_cents, compare_at_cents, currency, created_at, updated_at) values (?, ?, ?, ?, ?, 'INR', current_timestamp, current_timestamp)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, productId);
            ps.setString(2, sku);
            ps.setString(3, variantName);
            ps.setInt(4, request.getPriceCents());
            if (compareAtCents == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, compareAtCents);
            }
            return ps;
        }, variantKey);

        long variantId = Objects.requireNonNull(variantKey.getKey()).longValue();
        jdbcTemplate.update(
                "insert into inventory (variant_id, stock_qty, low_stock_threshold, is_backorder_allowed, updated_at) values (?, ?, 5, false, current_timestamp)",
                variantId,
                request.getStockQty()
        );

        syncProductTags(productId, normalizedTags);
        syncProductMedia(productId, normalizedMedia);

        return getProduct(productId);
    }

    @Override
    @Transactional
    public AdminProductDto updateProduct(Long productId, AdminProductRequest request) {
        AdminProductDto existing = getProduct(productId);
        long categoryId = ensureCategory(defaultIfBlank(request.getCategoryName(), defaultIfBlank(existing.getCategoryName(), "General")));
        Long brandId = ensureBrand(defaultIfBlank(request.getBrandName(), existing.getBrandName()));
        String slug = uniqueProductSlug(defaultIfBlank(request.getSlug(), slugify(request.getTitle())), productId);
        String status = normalizeProductStatus(request.getStatus());
        String variantName = defaultIfBlank(request.getVariantName(), request.getTitle());
        List<String> normalizedTags = normalizeTags(request.getTags());
        String coverImage = resolveCoverImage(request.getCoverImage(), request.getMedia());
        Integer compareAtCents = normalizeCompareAtCents(request.getCompareAtCents(), request.getPriceCents());
        if (coverImage == null) {
            coverImage = existing.getCoverImage();
        }
        List<AdminProductMediaDto> normalizedMedia = normalizeMedia(request.getMedia(), coverImage, request.getTitle());
        final String resolvedCoverImage = coverImage;

        Long variantId;
        try {
            variantId = jdbcTemplate.queryForObject(
                    "select id from product_variants where product_id = ? order by id asc limit 1",
                    Long.class,
                    productId
            );
        } catch (EmptyResultDataAccessException ex) {
            variantId = null;
        }
        String sku = uniqueSku(defaultIfBlank(request.getSku(), existing.getSku()), variantId);

        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "update products set category_id = ?, brand_id = ?, title = ?, slug = ?, description = ?, cover_image = ?, status = ?, updated_at = current_timestamp where id = ?"
            );
            ps.setLong(1, categoryId);
            if (brandId == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, brandId);
            }
            ps.setString(3, request.getTitle().trim());
            ps.setString(4, slug);
            ps.setString(5, emptyToNull(request.getDescription()));
            ps.setString(6, emptyToNull(resolvedCoverImage));
            ps.setString(7, status);
            ps.setLong(8, productId);
            return ps;
        });
        if (updated == 0) {
            throw new CustomException("Product not found", HttpStatus.NOT_FOUND);
        }

        if (variantId == null) {
            KeyHolder variantKey = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "insert into product_variants (product_id, sku, name, price_cents, compare_at_cents, currency, created_at, updated_at) values (?, ?, ?, ?, ?, 'INR', current_timestamp, current_timestamp)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, productId);
                ps.setString(2, sku);
                ps.setString(3, variantName);
                ps.setInt(4, request.getPriceCents());
                if (compareAtCents == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, compareAtCents);
                }
                return ps;
            }, variantKey);
            variantId = Objects.requireNonNull(variantKey.getKey()).longValue();
        } else {
            jdbcTemplate.update(
                    "update product_variants set sku = ?, name = ?, price_cents = ?, compare_at_cents = ?, updated_at = current_timestamp where id = ?",
                    sku,
                    variantName,
                    request.getPriceCents(),
                    compareAtCents,
                    variantId
            );
        }

        int inventoryUpdated = jdbcTemplate.update(
                "update inventory set stock_qty = ?, updated_at = current_timestamp where variant_id = ?",
                request.getStockQty(),
                variantId
        );
        if (inventoryUpdated == 0) {
            jdbcTemplate.update(
                    "insert into inventory (variant_id, stock_qty, low_stock_threshold, is_backorder_allowed, updated_at) values (?, ?, 5, false, current_timestamp)",
                    variantId,
                    request.getStockQty()
            );
        }

        syncProductTags(productId, normalizedTags);
        syncProductMedia(productId, normalizedMedia);

        return getProduct(productId);
    }

    @Override
    @Transactional
    public AdminProductDto updateProductStock(Long productId, AdminProductStockRequest request) {
        if (request == null) {
            throw new CustomException("Stock update details are required", HttpStatus.BAD_REQUEST);
        }

        Long variantId;
        try {
            variantId = jdbcTemplate.queryForObject(
                    "select id from product_variants where product_id = ? order by id asc limit 1",
                    Long.class,
                    productId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new CustomException("Product variant not found", HttpStatus.NOT_FOUND);
        }

        Integer currentStock;
        try {
            currentStock = jdbcTemplate.queryForObject(
                    "select stock_qty from inventory where variant_id = ? limit 1",
                    Integer.class,
                    variantId
            );
        } catch (EmptyResultDataAccessException ex) {
            currentStock = 0;
        }
        int baseStock = currentStock == null ? 0 : currentStock;
        Integer requestedStock = request.getStockQty();
        Integer stockDelta = request.getStockDelta();

        if (requestedStock == null && stockDelta == null) {
            throw new CustomException("Provide stock quantity or stock delta", HttpStatus.BAD_REQUEST);
        }

        int nextStock = requestedStock != null ? requestedStock : baseStock + stockDelta;
        if (nextStock < 0) {
            throw new CustomException("Stock cannot go below zero", HttpStatus.BAD_REQUEST);
        }

        int updated = jdbcTemplate.update(
                "update inventory set stock_qty = ?, updated_at = current_timestamp where variant_id = ?",
                nextStock,
                variantId
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "insert into inventory (variant_id, stock_qty, low_stock_threshold, is_backorder_allowed, updated_at) values (?, ?, 5, false, current_timestamp)",
                    variantId,
                    nextStock
            );
        }

        return getProduct(productId);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        int deleted = jdbcTemplate.update("delete from products where id = ?", productId);
        if (deleted == 0) {
            throw new CustomException("Product not found", HttpStatus.NOT_FOUND);
        }
    }

    private Optional<AdminOrderDto> getOrderById(Long orderId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    select o.id, o.order_number, o.status, o.total_cents, o.payment_status, o.placed_at,
                           u.email as user_email
                    from orders o
                    left join users u on u.id = o.user_id
                    where o.id = ?
                    """, orderRowMapper, orderId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<AdminProductDto> getProductById(Long productId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(productBaseQuery() + " where p.id = ?", productRowMapper, productId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private String productBaseQuery() {
        return """
                select p.id, p.title, p.slug, p.description, p.cover_image, p.status, p.created_at,
                       c.name as category_name,
                       b.name as brand_name,
                       pv.sku,
                       pv.name as variant_name,
                       coalesce(pv.price_cents, 0) as price_cents,
                       pv.compare_at_cents,
                       coalesce(i.stock_qty, 0) as stock_qty
                from products p
                left join categories c on c.id = p.category_id
                left join brands b on b.id = p.brand_id
                left join product_variants pv on pv.id = (
                    select pv2.id from product_variants pv2 where pv2.product_id = p.id order by pv2.id asc limit 1
                )
                left join inventory i on i.variant_id = pv.id
                """;
    }
    private long ensureCategory(String categoryName) {
        String name = defaultIfBlank(categoryName, "General").trim();
        String slug = uniqueCategorySlug(slugify(name));

        try {
            Long existingId = jdbcTemplate.queryForObject(
                    "select id from categories where lower(name) = lower(?) or slug = ? limit 1",
                    Long.class,
                    name,
                    slugify(name)
            );
            if (existingId != null) {
                return existingId;
            }
        } catch (EmptyResultDataAccessException ignored) {
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into categories (parent_id, name, slug, description, image_url, created_at, updated_at) values (null, ?, ?, null, null, current_timestamp, current_timestamp)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, slug);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private Long ensureBrand(String brandName) {
        String normalizedName = emptyToNull(brandName);
        if (normalizedName == null) {
            return null;
        }

        Optional<Long> existingId = findBrandId(normalizedName);
        if (existingId.isPresent()) {
            return existingId.get();
        }

        String slug = uniqueBrandSlug(slugify(normalizedName));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into brands (name, slug, created_at, updated_at) values (?, ?, current_timestamp, current_timestamp)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, normalizedName);
            ps.setString(2, slug);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private String uniqueCategorySlug(String baseSlug) {
        String candidate = defaultIfBlank(baseSlug, "general");
        int suffix = 1;
        while (countByQuery("select count(*) from categories where slug = ?", candidate) > 0) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String uniqueBrandSlug(String baseSlug) {
        String candidate = defaultIfBlank(baseSlug, "brand");
        int suffix = 1;
        while (countByQuery("select count(*) from brands where slug = ?", candidate) > 0) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String uniqueProductSlug(String requestedSlug, Long existingProductId) {
        String baseSlug = defaultIfBlank(slugify(requestedSlug), "product");
        String candidate = baseSlug;
        int suffix = 1;
        while (countProductSlugConflicts(candidate, existingProductId) > 0) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String uniqueSku(String requestedSku, Long existingVariantId) {
        String normalizedBase = defaultIfBlank(requestedSku, "SKU").trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "-");
        String candidate = normalizedBase;
        int suffix = 1;
        while (countSkuConflicts(candidate, existingVariantId) > 0) {
            candidate = normalizedBase + "-" + suffix++;
        }
        return candidate;
    }

    private long queryCount(String sql) {
        Number count = jdbcTemplate.queryForObject(sql, Number.class);
        return count == null ? 0L : count.longValue();
    }

    private long countByQuery(String sql, Object... args) {
        Number count = jdbcTemplate.queryForObject(sql, Number.class, args);
        return count == null ? 0L : count.longValue();
    }

    private long countProductSlugConflicts(String slug, Long existingProductId) {
        if (existingProductId == null) {
            return countByQuery("select count(*) from products where slug = ?", slug);
        }
        return countByQuery("select count(*) from products where slug = ? and id <> ?", slug, existingProductId);
    }

    private long countSkuConflicts(String sku, Long existingVariantId) {
        if (existingVariantId == null) {
            return countByQuery("select count(*) from product_variants where sku = ?", sku);
        }
        return countByQuery("select count(*) from product_variants where sku = ? and id <> ?", sku, existingVariantId);
    }

    private Optional<Long> findBrandId(String brandName) {
        String normalizedName = emptyToNull(brandName);
        if (normalizedName == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "select id from brands where lower(name) = lower(?) or slug = ? limit 1",
                    Long.class,
                    normalizedName,
                    slugify(normalizedName)
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
    private void hydrateProductRelationships(List<AdminProductDto> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        List<Long> productIds = products.stream()
                .map(AdminProductDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<String>> tagsByProductId = loadTagsByProductIds(productIds);
        Map<Long, List<AdminProductMediaDto>> mediaByProductId = loadMediaByProductIds(productIds);

        for (AdminProductDto product : products) {
            List<String> tags = tagsByProductId.getOrDefault(product.getId(), Collections.emptyList());
            List<AdminProductMediaDto> media = mediaByProductId.getOrDefault(product.getId(), Collections.emptyList());
            product.setTags(new ArrayList<>(tags));
            product.setMedia(new ArrayList<>(media));
            if ((product.getCoverImage() == null || product.getCoverImage().isBlank()) && !media.isEmpty()) {
                product.setCoverImage(media.get(0).getUrl());
            }
        }
    }

    private Map<Long, List<String>> loadTagsByProductIds(List<Long> productIds) {
        Map<Long, List<String>> tagsByProductId = new LinkedHashMap<>();
        if (productIds.isEmpty()) {
            return tagsByProductId;
        }

        jdbcTemplate.query(
                "select product_id, tag from product_tags where product_id in (" + placeholders(productIds.size()) + ") order by product_id asc, tag asc",
                (RowCallbackHandler) rs -> tagsByProductId.computeIfAbsent(rs.getLong("product_id"), ignored -> new ArrayList<>()).add(rs.getString("tag")),
                productIds.toArray()
        );
        return tagsByProductId;
    }

    private Map<Long, List<AdminProductMediaDto>> loadMediaByProductIds(List<Long> productIds) {
        Map<Long, List<AdminProductMediaDto>> mediaByProductId = new LinkedHashMap<>();
        if (productIds.isEmpty()) {
            return mediaByProductId;
        }

        jdbcTemplate.query(
                "select id, product_id, url, alt_text, sort_order from product_media where product_id in (" + placeholders(productIds.size()) + ") order by product_id asc, sort_order asc, id asc",
                (RowCallbackHandler) rs -> mediaByProductId.computeIfAbsent(rs.getLong("product_id"), ignored -> new ArrayList<>()).add(productMediaRowMapper.mapRow(rs, 0)),
                productIds.toArray()
        );
        return mediaByProductId;
    }

    private void syncProductTags(long productId, List<String> tags) {
        jdbcTemplate.update("delete from product_tags where product_id = ?", productId);
        for (String tag : tags) {
            jdbcTemplate.update("insert into product_tags (product_id, tag) values (?, ?)", productId, tag);
        }
    }

    private void syncProductMedia(long productId, List<AdminProductMediaDto> media) {
        jdbcTemplate.update("delete from product_media where product_id = ?", productId);
        for (AdminProductMediaDto item : media) {
            jdbcTemplate.update(
                    "insert into product_media (product_id, url, alt_text, sort_order) values (?, ?, ?, ?)",
                    productId,
                    item.getUrl(),
                    emptyToNull(item.getAltText()),
                    item.getSortOrder()
            );
        }
    }
    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = normalizeTag(tag);
            if (value != null) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    private Integer normalizeCompareAtCents(Integer compareAtCents, int priceCents) {
        if (compareAtCents == null || compareAtCents <= 0 || compareAtCents <= priceCents) {
            return null;
        }
        return compareAtCents;
    }

    private String normalizeTag(String tag) {
        String normalized = emptyToNull(tag);
        if (normalized == null) {
            return null;
        }

        String value = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return value.isBlank() ? null : value;
    }

    private String resolveCoverImage(String coverImage, List<AdminProductMediaDto> media) {
        String normalizedCover = emptyToNull(coverImage);
        if (normalizedCover != null) {
            return normalizedCover;
        }
        if (media == null) {
            return null;
        }
        for (AdminProductMediaDto item : media) {
            if (item == null) {
                continue;
            }
            String url = emptyToNull(item.getUrl());
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private List<AdminProductMediaDto> normalizeMedia(List<AdminProductMediaDto> media, String coverImage, String title) {
        LinkedHashMap<String, AdminProductMediaDto> mediaByUrl = new LinkedHashMap<>();

        if (coverImage != null) {
            AdminProductMediaDto cover = new AdminProductMediaDto();
            cover.setUrl(coverImage);
            cover.setAltText(title + " cover image");
            mediaByUrl.put(coverImage, cover);
        }

        if (media != null) {
            for (AdminProductMediaDto item : media) {
                if (item == null) {
                    continue;
                }

                String url = emptyToNull(item.getUrl());
                if (url == null) {
                    continue;
                }

                AdminProductMediaDto existing = mediaByUrl.get(url);
                if (existing == null) {
                    AdminProductMediaDto normalized = new AdminProductMediaDto();
                    normalized.setUrl(url);
                    normalized.setAltText(emptyToNull(item.getAltText()));
                    mediaByUrl.put(url, normalized);
                } else if ((existing.getAltText() == null || existing.getAltText().isBlank())
                        && item.getAltText() != null && !item.getAltText().isBlank()) {
                    existing.setAltText(item.getAltText().trim());
                }
            }
        }

        List<AdminProductMediaDto> normalizedMedia = new ArrayList<>();
        int sortOrder = 0;
        for (AdminProductMediaDto item : mediaByUrl.values()) {
            AdminProductMediaDto normalized = new AdminProductMediaDto();
            normalized.setUrl(item.getUrl());
            normalized.setAltText(defaultIfBlank(item.getAltText(), title + " gallery image " + (sortOrder + 1)));
            normalized.setSortOrder(sortOrder++);
            normalizedMedia.add(normalized);
        }
        return normalizedMedia;
    }

    private String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }
    private void syncShipmentStatus(Long orderId, String status) {
        switch (status) {
            case "pending", "failed", "cancelled" -> removePendingShipment(orderId);
            case "paid" -> ensureShipmentRecord(orderId);
            case "shipped" -> markShipmentInTransit(orderId);
            case "delivered" -> markShipmentDelivered(orderId);
            default -> {
            }
        }
    }

    private void ensureShipmentRecord(Long orderId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from shipments where order_id = ?", Integer.class, orderId);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("insert into shipments (order_id, status, updated_at) values (?, 'pending', current_timestamp)", orderId);
    }

    private void markShipmentInTransit(Long orderId) {
        ensureShipmentRecord(orderId);
        jdbcTemplate.update("""
                update shipments
                set status = 'in_transit',
                    shipped_at = coalesce(shipped_at, current_timestamp),
                    updated_at = current_timestamp
                where order_id = ?
                """, orderId);
    }

    private void markShipmentDelivered(Long orderId) {
        ensureShipmentRecord(orderId);
        jdbcTemplate.update("""
                update shipments
                set status = 'delivered',
                    shipped_at = coalesce(shipped_at, current_timestamp),
                    delivered_at = coalesce(delivered_at, current_timestamp),
                    updated_at = current_timestamp
                where order_id = ?
                """, orderId);
    }

    private void removePendingShipment(Long orderId) {
        jdbcTemplate.update("delete from shipments where order_id = ? and lower(status) = 'pending'", orderId);
    }

    private void sendDeliveredEmail(Long orderId) {
        String email = jdbcTemplate.queryForObject("""
                select u.email
                from orders o
                join users u on u.id = o.user_id
                where o.id = ?
                limit 1
                """, String.class, orderId);
        String orderNumber = jdbcTemplate.queryForObject("select order_number from orders where id = ? limit 1", String.class, orderId);
        if (email != null && !email.isBlank() && orderNumber != null && !orderNumber.isBlank()) {
            emailService.sendOrderDelivered(email, orderNumber);
        }
    }

    private RefundOutcome startRefund(CommerceSupport.OrderRecord order,
                                      CommerceSupport.PaymentRecord payment,
                                      String note) {
        try {
            PaymentGatewayClient.GatewayRefund gatewayRefund = paymentGatewayClient.refund(
                    payment.provider(),
                    payment.providerPaymentId(),
                    order.totalCents(),
                    order.currency(),
                    order.orderNumber(),
                    note
            );
            String refundStatus = normalizeLower(gatewayRefund.status());
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
                    "processed".equals(refundStatus) ? Timestamp.valueOf(java.time.LocalDateTime.now()) : null
            );
            String paymentStatus = "processed".equals(refundStatus) ? "refunded" : "refund_pending";
            String returnStatus = "processed".equals(refundStatus) ? "refunded" : "approved";
            String returnRefundStatus = "processed".equals(refundStatus) ? "processed" : "pending";
            return new RefundOutcome(paymentStatus, paymentStatus, returnStatus, returnRefundStatus, refundStatus);
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
            return new RefundOutcome("refund_pending", "refund_pending", "approved", "failed", "failed");
        }
    }

    private boolean hasCapturedPayment(CommerceSupport.PaymentRecord payment) {
        String status = normalizeLower(payment == null ? null : payment.status());
        return List.of("captured", "authorized").contains(status);
    }

    private boolean isRefundableProvider(String provider) {
        String normalized = normalizeLower(provider);
        return List.of("razorpay", "stripe").contains(normalized);
    }

    private void restoreStock(long orderId) {
        jdbcTemplate.query("""
                select variant_id, qty
                from order_items
                where order_id = ?
                order by id asc
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

    private String normalizeOrderStatus(String status) {
        String normalized = defaultIfBlank(status, "pending").trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_ORDER_STATUSES.contains(normalized)) {
            throw new CustomException("Unsupported order status", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeProductStatus(String status) {
        String normalized = defaultIfBlank(status, "active").trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_PRODUCT_STATUSES.contains(normalized)) {
            throw new CustomException("Unsupported product status", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String value = phone.trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLabel(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String slugify(String value) {
        String normalized = defaultIfBlank(value, "item")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "item" : normalized;
    }

    private String generateSku(String title) {
        return slugify(title).toUpperCase(Locale.ROOT).replace('-', '_') + "_" + System.currentTimeMillis();
    }

    private AdminUserDto mapSavedUser(User user) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        return dto;
    }

    private record RefundOutcome(String orderPaymentStatus,
                                 String paymentStatus,
                                 String returnStatus,
                                 String refundStatus,
                                 String refundMailStatus) {
    }

    @Override
    public List<ReturnRequestDto> getReturnRequests() {
        String sql = "SELECT rr.id, rr.order_id, o.user_id, o.order_number, rr.status, rr.refund_status, " +
                     "rr.reason, (o.total_cents / 100.0) as refund_amount_inr, rr.requested_at, rr.reviewed_at, " +
                     "rr.picked_at, rr.updated_at " +
                     "FROM return_requests rr " +
                     "JOIN orders o ON rr.order_id = o.id " +
                     "ORDER BY rr.requested_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new ReturnRequestDto(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getLong("user_id"),
                        rs.getString("order_number"),
                        rs.getString("status"),
                        rs.getString("refund_status"),
                        rs.getString("reason"),
                        rs.getDouble("refund_amount_inr"),
                        rs.getTimestamp("requested_at").toLocalDateTime(),
                        rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null,
                        rs.getTimestamp("picked_at") != null ? rs.getTimestamp("picked_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at").toLocalDateTime()
                )
        );
    }

    @Override
    public ReturnRequestDto getReturnRequest(Long returnRequestId) {
        String sql = "SELECT rr.id, rr.order_id, o.user_id, o.order_number, rr.status, rr.refund_status, " +
                     "rr.reason, (o.total_cents / 100.0) as refund_amount_inr, rr.requested_at, rr.reviewed_at, " +
                     "rr.picked_at, rr.updated_at " +
                     "FROM return_requests rr " +
                     "JOIN orders o ON rr.order_id = o.id " +
                     "WHERE rr.id = ?";

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                    new ReturnRequestDto(
                            rs.getLong("id"),
                            rs.getLong("order_id"),
                            rs.getLong("user_id"),
                            rs.getString("order_number"),
                            rs.getString("status"),
                            rs.getString("refund_status"),
                            rs.getString("reason"),
                            rs.getDouble("refund_amount_inr"),
                            rs.getTimestamp("requested_at").toLocalDateTime(),
                            rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null,
                            rs.getTimestamp("picked_at") != null ? rs.getTimestamp("picked_at").toLocalDateTime() : null,
                            rs.getTimestamp("updated_at").toLocalDateTime()
                    ), returnRequestId);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Return request not found", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public ReturnRequestDto updateReturnRequest(Long returnRequestId, AdminReturnUpdateRequest request) {
        String selectSql = "SELECT order_id FROM return_requests WHERE id = ?";
        Long orderId;
        try {
            orderId = jdbcTemplate.queryForObject(selectSql, Long.class, returnRequestId);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Return request not found", HttpStatus.NOT_FOUND);
        }

        String action = request.getAction().toLowerCase();
        
        if (action.equals("approve")) {
            // Approve return - initiate refund
            jdbcTemplate.update(
                    "UPDATE return_requests SET status = 'approved', reviewed_at = CURRENT_TIMESTAMP WHERE id = ?",
                    returnRequestId
            );

            // Get order details to start refund
            String orderSql = "SELECT id, user_id FROM orders WHERE id = ?";
            Map<String, Object> orderData = jdbcTemplate.queryForMap(orderSql, orderId);

        } else if (action.equals("accept")) {
            // Mark as accepted (pickup confirmed)
            jdbcTemplate.update(
                    "UPDATE return_requests SET status = 'accepted', picked_at = CURRENT_TIMESTAMP WHERE id = ?",
                    returnRequestId
            );
        } else if (action.equals("reject")) {
            // Reject the return request
            jdbcTemplate.update(
                    "UPDATE return_requests SET status = 'rejected', reviewed_at = CURRENT_TIMESTAMP WHERE id = ?",
                    returnRequestId
            );
        } else if (action.equals("complete")) {
            // Mark return as completed
            jdbcTemplate.update(
                    "UPDATE return_requests SET status = 'completed', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    returnRequestId
            );
        } else if (action.equals("cancel")) {
            // Cancel the return process
            jdbcTemplate.update(
                    "UPDATE return_requests SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    returnRequestId
            );
        }

        return getReturnRequest(returnRequestId);
    }

    @Override
    public RefundStatusDto getRefundStatus(Long orderId) {
        String sql = "SELECT id, order_id, payment_id, amount_cents, currency, status, provider_refund_id, note, " +
                     "requested_at, processed_at, updated_at " +
                     "FROM refunds WHERE order_id = ? ORDER BY requested_at DESC LIMIT 1";
        
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                    new RefundStatusDto(
                            rs.getLong("id"),
                            rs.getLong("order_id"),
                            rs.getLong("payment_id"),
                            rs.getInt("amount_cents"),
                            rs.getString("currency"),
                            rs.getString("status"),
                            rs.getString("provider_refund_id"),
                            rs.getString("note"),
                            rs.getTimestamp("requested_at").toLocalDateTime(),
                            rs.getTimestamp("processed_at") != null ? rs.getTimestamp("processed_at").toLocalDateTime() : null,
                            rs.getTimestamp("updated_at").toLocalDateTime()
                    ), orderId);
        } catch (EmptyResultDataAccessException e) {
            return null; // No refund found
        }
    }
}




