package com.nexbuy.ai.service.impl;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.product.dto.ProductDto;
import com.nexbuy.modules.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AiCommerceSupport {

    private static final Pattern BETWEEN_PRICE_PATTERN = Pattern.compile("between\\s+(?:rs\\.?\\s*)?([\\d,]+)\\s+(?:and|to)\\s+(?:rs\\.?\\s*)?([\\d,]+)");
    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile("(?:under|below|less than|max(?:imum)? of)\\s+(?:rs\\.?\\s*)?([\\d,]+)");
    private static final Pattern MIN_PRICE_PATTERN = Pattern.compile("(?:above|over|more than|starting at|from)\\s+(?:rs\\.?\\s*)?([\\d,]+)");
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("\\b[A-Z]{2,}-[A-Z0-9-]{4,}\\b");
    private static final Set<String> IMAGE_STOP_WORDS = Set.of(
            "image", "img", "photo", "picture", "pic", "screenshot", "screen", "snap", "copy",
            "download", "received", "whatsapp", "camera", "upload", "jpeg", "jpg", "png", "webp"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ProductService productService;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public AiCommerceSupport(JdbcTemplate jdbcTemplate, ProductService productService) {
        this.jdbcTemplate = jdbcTemplate;
        this.productService = productService;
        this.currency.setMaximumFractionDigits(2);
        this.currency.setMinimumFractionDigits(2);
    }

    public Long findUserId(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        List<Long> matches = jdbcTemplate.query(
                """
                        select id
                        from users
                        where lower(email) = lower(?)
                        limit 1
                        """,
                (rs, rowNum) -> rs.getLong("id"),
                email.trim()
        );
        return matches.isEmpty() ? null : matches.get(0);
    }

    public void logAiRequest(Long userId, String type, String requestPayload, String responseRef) {
        jdbcTemplate.update(
                """
                        insert into ai_requests (user_id, type, request_payload, response_ref, created_at)
                        values (?, ?, ?, ?, current_timestamp)
                        """,
                userId,
                safeEnum(type),
                toJsonPayload(requestPayload),
                truncate(responseRef, 1000)
        );
    }

    public void logSearch(Long userId, String query, String type, int resultCount) {
        jdbcTemplate.update(
                """
                        insert into searches (user_id, query_text, type, result_count, created_at)
                        values (?, ?, ?, ?, current_timestamp)
                        """,
                userId,
                truncate(query == null || query.isBlank() ? "ai search" : query.trim(), 255),
                safeEnum(type),
                resultCount
        );
    }

    public ShoppingIntent interpretShoppingIntent(String rawText) {
        String normalized = normalize(rawText);
        if (normalized == null || normalized.isEmpty()) {
            return new ShoppingIntent("", null, null, null, null, null, true, "relevance", List.of());
        }

        String category = firstMatchingToken(normalized, loadKnownCategories());
        String brand = firstMatchingToken(normalized, loadKnownBrands());
        String tag = firstMatchingToken(normalized, loadKnownTags());

        Integer minPrice = null;
        Integer maxPrice = null;

        Matcher betweenMatcher = BETWEEN_PRICE_PATTERN.matcher(normalized);
        if (betweenMatcher.find()) {
            minPrice = parseRupeesToCents(betweenMatcher.group(1));
            maxPrice = parseRupeesToCents(betweenMatcher.group(2));
        } else {
            Matcher maxMatcher = MAX_PRICE_PATTERN.matcher(normalized);
            if (maxMatcher.find()) {
                maxPrice = parseRupeesToCents(maxMatcher.group(1));
            }
            Matcher minMatcher = MIN_PRICE_PATTERN.matcher(normalized);
            if (minMatcher.find()) {
                minPrice = parseRupeesToCents(minMatcher.group(1));
            }
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            int swap = minPrice;
            minPrice = maxPrice;
            maxPrice = swap;
        }

        boolean inStock = containsAny(normalized, "in stock", "available", "ready to ship", "available now", "instock");
        String sort = "relevance";
        if (containsAny(normalized, "budget", "cheap", "cheapest", "best deal", "lowest", "low price", "affordable")) {
            sort = "price_asc";
        } else if (containsAny(normalized, "premium", "flagship", "best", "top end", "high end")) {
            sort = "price_desc";
        } else if (containsAny(normalized, "new", "latest", "newest", "launch")) {
            sort = "newest";
        }

        String cleanedQuery = stripNoise(normalized, category, brand, tag);
        List<String> matchedSignals = new ArrayList<>();
        if (category != null) {
            matchedSignals.add("Category: " + pretty(category));
        }
        if (brand != null) {
            matchedSignals.add("Brand: " + pretty(brand));
        }
        if (tag != null) {
            matchedSignals.add("Tag: " + pretty(tag));
        }
        if (minPrice != null || maxPrice != null) {
            matchedSignals.add("Budget filter");
        }
        if (inStock) {
            matchedSignals.add("Only in-stock items");
        }

        return new ShoppingIntent(cleanedQuery, category, brand, tag, minPrice, maxPrice, inStock, sort, matchedSignals);
    }

    public List<ProductDto.ProductCard> searchProducts(ShoppingIntent intent, int limit) {
        ProductDto.ProductListResponse response = productService.getCatalog(
                emptyToNull(intent.query()),
                intent.category(),
                intent.brand(),
                intent.tag(),
                intent.minPrice(),
                intent.maxPrice(),
                intent.inStock(),
                intent.sort(),
                1,
                Math.max(1, limit)
        );
        return response.items();
    }

    public String summarizeShoppingIntent(ShoppingIntent intent, int resultsCount) {
        List<String> parts = new ArrayList<>();
        if (intent.category() != null) {
            parts.add(pretty(intent.category()));
        }
        if (intent.brand() != null) {
            parts.add(pretty(intent.brand()));
        }
        if (intent.tag() != null) {
            parts.add(pretty(intent.tag()));
        }
        if (!intent.query().isBlank()) {
            parts.add("\"" + intent.query() + "\"");
        }

        String focus = parts.isEmpty() ? "the live catalog" : String.join(" / ", parts);
        List<String> modifiers = new ArrayList<>();
        if (intent.minPrice() != null && intent.maxPrice() != null) {
            modifiers.add("between " + formatPrice(intent.minPrice()) + " and " + formatPrice(intent.maxPrice()));
        } else if (intent.maxPrice() != null) {
            modifiers.add("under " + formatPrice(intent.maxPrice()));
        } else if (intent.minPrice() != null) {
            modifiers.add("above " + formatPrice(intent.minPrice()));
        }
        if (Boolean.TRUE.equals(intent.inStock())) {
            modifiers.add("currently in stock");
        }

        String modifierText = modifiers.isEmpty() ? "" : " " + String.join(", ", modifiers);
        return "Found " + resultsCount + " matches for " + focus + modifierText + ".";
    }

    public RecommendationBundle buildRecommendations(String email, int limit) {
        Long userId = findUserId(email);
        if (userId == null) {
            List<ProductDto.ProductCard> fallback = getTrendingProducts(limit);
            return new RecommendationBundle(
                    false,
                    "Popular right now",
                    "These picks come from what shoppers are opening and buying most often across the live catalog.",
                    List.of("Trending across the storefront", "In-stock now", "Strong mix of value and freshness"),
                    fallback
            );
        }

        ActivitySignals signals = loadActivitySignals(userId);
        List<ProductDto.ProductCard> combined = new ArrayList<>();

        if (signals.searchTerm() != null) {
            combined.addAll(productService.search(signals.searchTerm(), signals.cartCategory(), signals.brand(), signals.tag(), null, null, true, "relevance", 1, limit).items());
        }
        if (signals.cartCategory() != null) {
            combined.addAll(productService.getCatalog(null, signals.cartCategory(), signals.brand(), signals.tag(), null, null, true, "newest", 1, limit).items());
        }
        if (signals.category() != null) {
            combined.addAll(productService.getCatalog(null, signals.category(), signals.brand(), signals.tag(), null, null, true, "newest", 1, limit).items());
        }
        if (signals.brand() != null) {
            combined.addAll(productService.getCatalog(null, null, signals.brand(), signals.tag(), null, null, true, "newest", 1, limit).items());
        }
        if (signals.tag() != null) {
            combined.addAll(productService.getCatalog(null, null, null, signals.tag(), null, null, true, "relevance", 1, limit).items());
        }
        combined.addAll(getTrendingProducts(limit));

        List<ProductDto.ProductCard> products = dedupeProducts(combined, limit);
        boolean personalized = signals.hasSignals() && !products.isEmpty();

        if (!personalized) {
            return new RecommendationBundle(
                    false,
                    "Popular right now",
                    "I do not have enough recent activity to personalize deeply yet, so I am starting with strong live catalog picks.",
                    List.of("Based on current storefront demand"),
                    products
            );
        }

        List<String> reasons = new ArrayList<>();
        if (signals.cartCategory() != null) {
            reasons.add("Your active cart leans toward " + pretty(signals.cartCategory()));
        }
        if (signals.category() != null && !Objects.equals(signals.category(), signals.cartCategory())) {
            reasons.add("Your recent orders favor " + pretty(signals.category()));
        }
        if (signals.brand() != null) {
            reasons.add("You keep returning to " + pretty(signals.brand()));
        }
        if (signals.tag() != null) {
            reasons.add("Products tagged with " + pretty(signals.tag()) + " perform well in your history");
        }
        if (signals.searchTerm() != null) {
            reasons.add("Your latest searches mention \"" + signals.searchTerm() + "\"");
        }

        return new RecommendationBundle(
                true,
                "Picked for your shopping pattern",
                "These recommendations are driven by your recent orders, cart activity, and searches in the live store.",
                reasons,
                products
        );
    }

    public List<AiRequest.OrderPreview> loadRecentOrders(String email, int limit) {
        if (email == null || email.isBlank()) {
            return Collections.emptyList();
        }
        return jdbcTemplate.query(
                """
                        select o.order_number,
                               o.status,
                               o.payment_status,
                               o.placed_at
                        from orders o
                        join users u on u.id = o.user_id
                        where lower(u.email) = lower(?)
                        order by o.placed_at desc
                        limit ?
                        """,
                (rs, rowNum) -> new AiRequest.OrderPreview(
                        rs.getString("order_number"),
                        pretty(rs.getString("status")),
                        pretty(rs.getString("payment_status")),
                        rs.getTimestamp("placed_at") == null ? null : rs.getTimestamp("placed_at").toLocalDateTime()
                ),
                email.trim(),
                Math.max(1, limit)
        );
    }

    public Optional<AiRequest.OrderPreview> findMentionedOrder(String email, String message) {
        if (email == null || email.isBlank() || message == null || message.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = ORDER_NUMBER_PATTERN.matcher(message.toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            return Optional.empty();
        }
        String orderNumber = matcher.group();
        List<AiRequest.OrderPreview> matches = jdbcTemplate.query(
                """
                        select o.order_number,
                               o.status,
                               o.payment_status,
                               o.placed_at
                        from orders o
                        join users u on u.id = o.user_id
                        where lower(u.email) = lower(?)
                          and upper(o.order_number) = ?
                        limit 1
                        """,
                (rs, rowNum) -> new AiRequest.OrderPreview(
                        rs.getString("order_number"),
                        pretty(rs.getString("status")),
                        pretty(rs.getString("payment_status")),
                        rs.getTimestamp("placed_at") == null ? null : rs.getTimestamp("placed_at").toLocalDateTime()
                ),
                email.trim(),
                orderNumber
        );
        return matches.stream().findFirst();
    }

    public ImageInsight analyzeImage(MultipartFile file, String hint) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("Add an image before searching", HttpStatus.BAD_REQUEST);
        }

        String filename = Optional.ofNullable(file.getOriginalFilename())
                .map(Paths::get)
                .map(Path::getFileName)
                .map(path -> path.toString())
                .orElse("");

        List<String> filenameTokens = tokenizeImageHint(filename);
        List<String> hintTokens = tokenizeImageHint(hint);
        List<String> tokens = new ArrayList<>();
        tokens.addAll(hintTokens);
        tokens.addAll(filenameTokens);

        String extractedHint = tokens.stream().limit(6).collect(Collectors.joining(" "));
        List<String> palette = extractPalette(file);

        String confidence;
        if (!hintTokens.isEmpty()) {
            confidence = "high";
        } else if (tokens.size() >= 2) {
            confidence = "guided";
        } else {
            confidence = "fallback";
        }

        return new ImageInsight(
                emptyToNull(extractedHint),
                List.copyOf(palette),
                confidence,
                filename
        );
    }

    public List<ProductDto.ProductCard> searchByImageInsight(ImageInsight insight, int limit) {
        if (insight.extractedHint() != null) {
            List<ProductDto.ProductCard> matches = productService.search(
                    insight.extractedHint(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    "relevance",
                    1,
                    limit
            ).items();
            if (!matches.isEmpty()) {
                return matches;
            }
        }
        return getTrendingProducts(limit);
    }

    public List<ProductDto.ProductCard> getTrendingProducts(int limit) {
        return productService.getCatalog(null, null, null, null, null, null, true, "newest", 1, Math.max(1, limit)).items();
    }

    public String formatPrice(Integer cents) {
        if (cents == null) {
            return "";
        }
        return currency.format(cents / 100.0);
    }

    private ActivitySignals loadActivitySignals(long userId) {
        String cartCategory = queryForString(
                """
                        select c.slug
                        from carts ct
                        join cart_items ci on ci.cart_id = ct.id
                        join product_variants pv on pv.id = ci.variant_id
                        join products p on p.id = pv.product_id
                        join categories c on c.id = p.category_id
                        where ct.user_id = ?
                          and lower(ct.status) = 'active'
                        group by c.slug
                        order by sum(ci.qty) desc, max(ci.updated_at) desc
                        limit 1
                        """,
                userId
        );
        String category = queryForString(
                """
                        select c.slug
                        from orders o
                        join order_items oi on oi.order_id = o.id
                        join product_variants pv on pv.id = oi.variant_id
                        join products p on p.id = pv.product_id
                        join categories c on c.id = p.category_id
                        where o.user_id = ?
                          and lower(o.status) not in ('cancelled', 'failed')
                        group by c.slug
                        order by sum(oi.qty) desc, max(o.placed_at) desc
                        limit 1
                        """,
                userId
        );
        String brand = queryForString(
                """
                        select b.slug
                        from orders o
                        join order_items oi on oi.order_id = o.id
                        join product_variants pv on pv.id = oi.variant_id
                        join products p on p.id = pv.product_id
                        join brands b on b.id = p.brand_id
                        where o.user_id = ?
                          and lower(o.status) not in ('cancelled', 'failed')
                          and b.slug is not null
                        group by b.slug
                        order by sum(oi.qty) desc, max(o.placed_at) desc
                        limit 1
                        """,
                userId
        );
        String tag = queryForString(
                """
                        select pt.tag
                        from orders o
                        join order_items oi on oi.order_id = o.id
                        join product_variants pv on pv.id = oi.variant_id
                        join products p on p.id = pv.product_id
                        join product_tags pt on pt.product_id = p.id
                        where o.user_id = ?
                          and lower(o.status) not in ('cancelled', 'failed')
                        group by pt.tag
                        order by sum(oi.qty) desc, max(o.placed_at) desc
                        limit 1
                        """,
                userId
        );
        String searchTerm = queryForString(
                """
                        select query_text
                        from searches
                        where user_id = ?
                        order by created_at desc
                        limit 1
                        """,
                userId
        );

        return new ActivitySignals(cartCategory, category, brand, normalize(tag), normalize(searchTerm));
    }

    private List<String> loadKnownCategories() {
        return jdbcTemplate.query(
                "select slug from categories order by name asc",
                (rs, rowNum) -> normalize(rs.getString("slug"))
        );
    }

    private List<String> loadKnownBrands() {
        return jdbcTemplate.query(
                "select slug from brands order by name asc",
                (rs, rowNum) -> normalize(rs.getString("slug"))
        );
    }

    private List<String> loadKnownTags() {
        return jdbcTemplate.query(
                """
                        select lower(tag) as tag
                        from product_tags
                        group by lower(tag)
                        order by count(*) desc, lower(tag) asc
                        limit 120
                        """,
                (rs, rowNum) -> normalize(rs.getString("tag"))
        );
    }

    private String firstMatchingToken(String text, List<String> values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String spaced = value.replace('-', ' ');
            if (text.contains(value) || text.contains(spaced)) {
                return value;
            }
        }
        return null;
    }

    private String stripNoise(String text, String category, String brand, String tag) {
        String cleaned = text;
        for (String value : new String[]{category, brand, tag}) {
            if (value != null) {
                cleaned = cleaned.replace(value, " ");
                cleaned = cleaned.replace(value.replace('-', ' '), " ");
            }
        }
        cleaned = cleaned
                .replaceAll("between\\s+(?:rs\\.?\\s*)?[\\d,]+\\s+(?:and|to)\\s+(?:rs\\.?\\s*)?[\\d,]+", " ")
                .replaceAll("(?:under|below|less than|max(?:imum)? of|above|over|more than|starting at|from)\\s+(?:rs\\.?\\s*)?[\\d,]+", " ")
                .replaceAll("\\b(show|find|need|want|looking|for|please|me|some|products|product|buy|shop|search|recommend|suggest|with|and|the|a|an|latest|best|cheap|budget|premium|available|in|stock|ready|to|ship)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned;
    }

    private Integer parseRupeesToCents(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String digits = raw.replace(",", "").trim();
            return Integer.parseInt(digits) * 100;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean containsAny(String text, String... values) {
        return Arrays.stream(values).anyMatch(text::contains);
    }

    private List<ProductDto.ProductCard> dedupeProducts(List<ProductDto.ProductCard> products, int limit) {
        Map<String, ProductDto.ProductCard> bySlug = new LinkedHashMap<>();
        for (ProductDto.ProductCard product : products) {
            if (product == null || product.slug() == null) {
                continue;
            }
            bySlug.putIfAbsent(product.slug(), product);
            if (bySlug.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(bySlug.values());
    }

    private List<String> tokenizeImageHint(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        String normalized = value
                .replaceAll("\\.[A-Za-z0-9]+$", " ")
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() > 2)
                .filter(token -> !IMAGE_STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private List<String> extractPalette(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                return List.of();
            }

            Map<Integer, Integer> buckets = new LinkedHashMap<>();
            int widthStep = Math.max(1, image.getWidth() / 36);
            int heightStep = Math.max(1, image.getHeight() / 36);

            for (int y = 0; y < image.getHeight(); y += heightStep) {
                for (int x = 0; x < image.getWidth(); x += widthStep) {
                    int rgb = image.getRGB(x, y);
                    int red = ((rgb >> 16) & 0xFF) / 32;
                    int green = ((rgb >> 8) & 0xFF) / 32;
                    int blue = (rgb & 0xFF) / 32;
                    int bucket = (red << 10) | (green << 5) | blue;
                    buckets.merge(bucket, 1, Integer::sum);
                }
            }

            return buckets.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .map(this::bucketToHex)
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private String bucketToHex(Integer bucket) {
        int red = ((bucket >> 10) & 0x1F) * 32 + 16;
        int green = ((bucket >> 5) & 0x1F) * 32 + 16;
        int blue = (bucket & 0x1F) * 32 + 16;
        return "#%02X%02X%02X".formatted(clampColor(red), clampColor(green), clampColor(blue));
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private String queryForString(String sql, Object... args) {
        List<String> matches = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), args);
        if (matches.isEmpty()) {
            return null;
        }
        return normalize(matches.get(0));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String pretty(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Arrays.stream(value.replace('-', ' ').split("\\s+"))
                .filter(token -> !token.isBlank())
                .map(token -> Character.toUpperCase(token.charAt(0)) + token.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String safeEnum(String value) {
        return normalize(value).replace('-', '_');
    }

    private String toJsonPayload(String payload) {
        String safe = payload == null ? "" : payload;
        return "{\"text\":\"" + safe
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ") + "\"}";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ShoppingIntent(String query,
                                 String category,
                                 String brand,
                                 String tag,
                                 Integer minPrice,
                                 Integer maxPrice,
                                 Boolean inStock,
                                 String sort,
                                 List<String> matchedSignals) {
    }

    public record RecommendationBundle(boolean personalized,
                                       String headline,
                                       String summary,
                                       List<String> signals,
                                       List<ProductDto.ProductCard> products) {
    }

    public record ImageInsight(String extractedHint,
                               List<String> palette,
                               String confidence,
                               String fileName) {
    }

    private record ActivitySignals(String cartCategory,
                                   String category,
                                   String brand,
                                   String tag,
                                   String searchTerm) {
        private boolean hasSignals() {
            return cartCategory != null || category != null || brand != null || tag != null || searchTerm != null;
        }
    }
}
