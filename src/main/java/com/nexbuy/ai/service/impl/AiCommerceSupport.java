package com.nexbuy.ai.service.impl;

import com.nexbuy.ai.client.OpenAIClient;
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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AiCommerceSupport {

    private static final String PRICE_TOKEN = "([\\d,.]+\\s*(?:k|thousand|lakh|lakhs|lac)?)";
    private static final Pattern BETWEEN_PRICE_PATTERN = Pattern.compile("between\\s+(?:rs\\.?\\s*)?" + PRICE_TOKEN + "\\s+(?:and|to)\\s+(?:rs\\.?\\s*)?" + PRICE_TOKEN);
    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile("(?:under|below|less than|max(?:imum)? of)\\s+(?:rs\\.?\\s*)?" + PRICE_TOKEN);
    private static final Pattern MIN_PRICE_PATTERN = Pattern.compile("(?:above|over|more than|starting at|from)\\s+(?:rs\\.?\\s*)?" + PRICE_TOKEN);
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("\\b[A-Z]{2,}-[A-Z0-9-]{4,}\\b");
    private static final Set<String> IMAGE_STOP_WORDS = Set.of(
            "image", "img", "photo", "picture", "pic", "screenshot", "screen", "snap", "copy",
            "download", "received", "whatsapp", "camera", "upload", "jpeg", "jpg", "png", "webp"
    );
    private static final Set<String> TEXT_STOP_WORDS = Set.of(
            "show", "find", "need", "want", "looking", "please", "products", "product", "buy", "shop", "search",
            "recommend", "suggest", "with", "and", "the", "a", "an", "latest", "best", "cheap", "budget", "premium",
            "available", "in", "stock", "ready", "to", "ship", "for", "me", "some", "give", "help",
            "about", "this", "that", "new", "top", "good", "better", "from", "under", "below", "over"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ProductService productService;
    private final OpenAIClient openAIClient;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Map<String, Optional<VisualSignature>> remoteImageSignatureCache = new ConcurrentHashMap<>();

    public AiCommerceSupport(JdbcTemplate jdbcTemplate, ProductService productService, OpenAIClient openAIClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.productService = productService;
        this.openAIClient = openAIClient;
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

        List<String> categories = loadKnownCategories();
        List<String> brands = loadKnownBrands();
        List<String> tags = loadKnownTags();

        String category = firstMatchingToken(normalized, categories);
        String brand = firstMatchingToken(normalized, brands);
        String tag = firstMatchingToken(normalized, tags);

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

        OpenAIClient.SearchInference aiInference = openAIClient
                .interpretSearch(rawText, categories, brands, tags)
                .orElse(null);

        category = mergeKnownValue(category, aiInference == null ? null : aiInference.category(), categories);
        brand = mergeKnownValue(brand, aiInference == null ? null : aiInference.brand(), brands);
        tag = mergeKnownValue(tag, aiInference == null ? null : aiInference.tag(), tags);

        if (minPrice == null && aiInference != null && aiInference.minPriceRupees() != null) {
            minPrice = aiInference.minPriceRupees() * 100;
        }
        if (maxPrice == null && aiInference != null && aiInference.maxPriceRupees() != null) {
            maxPrice = aiInference.maxPriceRupees() * 100;
        }
        if (!inStock && aiInference != null && Boolean.TRUE.equals(aiInference.inStock())) {
            inStock = true;
        }
        if ("relevance".equals(sort) && aiInference != null && aiInference.sort() != null) {
            sort = normalizeSort(aiInference.sort());
        }

        String cleanedQuery = stripNoise(normalized, category, brand, tag);
        String aiQuery = normalize(aiInference == null ? null : aiInference.query());
        if (aiQuery != null && !aiQuery.isBlank()) {
            cleanedQuery = aiQuery;
        } else if (cleanedQuery.isBlank()) {
            cleanedQuery = tightenQuery(normalized, category, brand, tag);
        }
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
        if (aiInference != null && aiInference.reason() != null) {
            matchedSignals.add("AI refined: " + aiInference.reason());
        }

        return new ShoppingIntent(cleanedQuery, category, brand, tag, minPrice, maxPrice, inStock, sort, matchedSignals);
    }

    public SearchPlan buildSearchPlan(String rawText, int limit) {
        ShoppingIntent interpreted = interpretShoppingIntent(rawText);
        ShoppingIntent effective = interpreted;
        List<ProductDto.ProductCard> products = searchProducts(interpreted, limit);
        boolean relaxed = false;
        String summary = summarizeShoppingIntent(interpreted, products.size());

        if (products.isEmpty()) {
            effective = relaxShoppingIntent(rawText, interpreted);
            if (!effective.equals(interpreted)) {
                products = searchProducts(effective, limit);
                relaxed = !products.isEmpty();
            }
        }

        if (products.isEmpty()) {
            products = getTrendingProducts(limit);
            effective = new ShoppingIntent(
                    effective.query(),
                    effective.category(),
                    effective.brand(),
                    effective.tag(),
                    null,
                    null,
                    true,
                    "newest",
                    appendSignal(effective.matchedSignals(), "Showing the strongest live catalog options")
            );
            summary = "I could not find an exact live match, so I am showing the strongest available options instead.";
            relaxed = true;
        } else if (relaxed) {
            summary = summarizeRelaxedSearch(interpreted, effective, products.size());
        }

        return new SearchPlan(rawText, interpreted, effective, products, relaxed, summary);
    }

    public AiRequest.SearchInterpretation toInterpretation(ShoppingIntent intent) {
        return new AiRequest.SearchInterpretation(
                emptyToNull(intent.query()),
                intent.category(),
                intent.brand(),
                intent.tag(),
                intent.minPrice(),
                intent.maxPrice(),
                intent.inStock(),
                intent.sort()
        );
    }

    public String buildCatalogUrl(ShoppingIntent intent, String fallbackQuery) {
        List<String> params = new ArrayList<>();
        String query = emptyToNull(intent.query());
        if (query == null) {
            query = emptyToNull(fallbackQuery);
        }

        if (query != null) {
            params.add("q=" + urlEncode(query));
        }
        if (intent.brand() != null) {
            params.add("brand=" + urlEncode(intent.brand()));
        }
        if (intent.tag() != null) {
            params.add("tag=" + urlEncode(intent.tag()));
        }
        if (intent.minPrice() != null) {
            params.add("minPrice=" + intent.minPrice());
        }
        if (intent.maxPrice() != null) {
            params.add("maxPrice=" + intent.maxPrice());
        }
        if (Boolean.TRUE.equals(intent.inStock())) {
            params.add("inStock=true");
        }
        if (intent.sort() != null && !"relevance".equals(intent.sort()) && !"newest".equals(intent.sort())) {
            params.add("sort=" + urlEncode(intent.sort()));
        } else if (intent.sort() != null && query != null && !"relevance".equals(intent.sort())) {
            params.add("sort=" + urlEncode(intent.sort()));
        }

        String querySuffix = params.isEmpty() ? "" : "?" + String.join("&", params);
        if (query == null && intent.category() != null) {
            return "/product/category/" + urlEncode(intent.category()) + querySuffix;
        }
        if (!querySuffix.isEmpty() || query != null) {
            return "/product/search" + querySuffix;
        }
        return "/product/catalog";
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
        if (hasText(intent.query())) {
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

        List<String> categories = loadKnownCategories();
        List<String> brands = loadKnownBrands();
        List<String> tags = loadKnownTags();
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException ex) {
            throw new CustomException("Could not read the uploaded image", HttpStatus.BAD_REQUEST);
        }

        String filename = Optional.ofNullable(file.getOriginalFilename())
                .map(Paths::get)
                .map(Path::getFileName)
                .map(path -> path.toString())
                .orElse("");

        List<String> hintTokens = tokenizeImageHint(hint);
        List<String> tokens = new ArrayList<>();
        tokens.addAll(hintTokens);

        OpenAIClient.ImageInference aiInference = Optional.ofNullable(file.getContentType())
                .flatMap(contentType -> {
                    return openAIClient.analyzeProductImage(imageBytes, contentType, null, hint, categories, brands, tags);
                })
                .orElseGet(() -> openAIClient.analyzeProductImage(imageBytes, null, null, hint, categories, brands, tags).orElse(null));

        BufferedImage image = readImage(imageBytes);
        VisualSignature uploadedSignature = buildVisualSignature(image);
        String visualCategory = uploadedSignature == null ? null : uploadedSignature.suggestedCategory();

        String category = mergeKnownValue(firstMatchingToken(String.join(" ", tokens), categories), aiInference == null ? null : aiInference.category(), categories);
        category = mergeKnownValue(category, visualCategory, categories);
        if (category == null) {
            category = inferCatalogCategory(uploadedSignature, categories);
        }
        String brand = mergeKnownValue(firstMatchingToken(String.join(" ", tokens), brands), aiInference == null ? null : aiInference.brand(), brands);
        List<String> resolvedTags = new ArrayList<>();
        if (aiInference != null) {
            for (String aiTag : aiInference.tags()) {
                String matchedTag = mergeKnownValue(null, aiTag, tags);
                if (matchedTag != null && !resolvedTags.contains(matchedTag)) {
                    resolvedTags.add(matchedTag);
                }
            }
        }
        if (resolvedTags.isEmpty()) {
            resolvedTags.addAll(inferVisualTags(category, image, tags));
        }

        String heuristicHint = tokens.stream().limit(6).collect(Collectors.joining(" "));
        String extractedHint = joinHints(aiInference == null ? null : aiInference.searchHint(), heuristicHint);
        if ((extractedHint == null || extractedHint.isBlank()) && category != null) {
            extractedHint = category.replace('-', ' ');
        }
        List<String> palette = extractPalette(image);
        List<String> colors = aiInference == null ? palette.stream().map(this::hexToColorName).toList() : aiInference.colors();

        String confidence;
        if (aiInference != null && aiInference.confidence() != null) {
            confidence = aiInference.confidence();
        } else if (visualCategory != null) {
            confidence = "guided";
        } else if (!hintTokens.isEmpty()) {
            confidence = "high";
        } else if (tokens.size() >= 2) {
            confidence = "guided";
        } else {
            confidence = "fallback";
        }

        return new ImageInsight(
                emptyToNull(extractedHint),
                category,
                brand,
                List.copyOf(resolvedTags),
                List.copyOf(colors),
                List.copyOf(palette),
                confidence,
                filename,
                uploadedSignature
        );
    }

    public ImageSearchMatch searchByImageInsight(ImageInsight insight, int limit) {
        ShoppingIntent imageIntent = toImageShoppingIntent(insight);
        List<ProductDto.ProductCard> candidates = new ArrayList<>();
        int candidatePool = Math.max(limit * 6, 48);

        if (imageIntent.category() != null || imageIntent.brand() != null || imageIntent.tag() != null || hasText(imageIntent.query())) {
            candidates.addAll(productService.getCatalog(
                    emptyToNull(imageIntent.query()),
                    imageIntent.category(),
                    imageIntent.brand(),
                    imageIntent.tag(),
                    null,
                    null,
                    true,
                    "relevance",
                    1,
                    candidatePool
            ).items());
        }

        if (hasText(imageIntent.query())) {
            candidates.addAll(productService.search(
                    imageIntent.query(),
                    imageIntent.category(),
                    imageIntent.brand(),
                    imageIntent.tag(),
                    null,
                    null,
                    true,
                    "relevance",
                    1,
                    candidatePool
            ).items());
        }

        if (candidates.isEmpty() && imageIntent.category() != null) {
            candidates.addAll(productService.getCatalog(
                    null,
                    imageIntent.category(),
                    imageIntent.brand(),
                    imageIntent.tag(),
                    null,
                    null,
                    true,
                    "newest",
                    1,
                    candidatePool
            ).items());
        }

        if (candidates.isEmpty() || insight.visualSignature() != null) {
            candidates.addAll(productService.getCatalog(
                    null,
                    imageIntent.category(),
                    null,
                    null,
                    null,
                    null,
                    true,
                    "newest",
                    1,
                    candidatePool
            ).items());
        }

        if (candidates.isEmpty()) {
            candidates.addAll(productService.getCatalog(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    "newest",
                    1,
                    candidatePool
            ).items());
        }

        List<ProductDto.ProductCard> deduped = dedupeProducts(candidates, candidatePool);
        List<ScoredProduct> scoredProducts = deduped.stream()
                .map(product -> new ScoredProduct(product, scoreProductForImage(product, insight)))
                .filter(scored -> scored.score() >= 60)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .limit(Math.max(1, limit))
                .toList();

        if (!scoredProducts.isEmpty()) {
            return new ImageSearchMatch(
                    scoredProducts.stream().map(ScoredProduct::product).toList(),
                    false,
                    60
            );
        }

        if (imageIntent.category() != null) {
            List<ScoredProduct> sameCategoryProducts = deduped.stream()
                    .filter(product -> matchesField(product.categorySlug(), imageIntent.category(), product.categoryName()))
                    .map(product -> new ScoredProduct(product, scoreProductForImage(product, insight)))
                    .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                    .toList();
            List<ScoredProduct> similarProducts = sameCategoryProducts.stream()
                    .filter(scored -> scored.score() >= 35)
                    .limit(Math.max(1, limit))
                    .toList();
            if (!similarProducts.isEmpty()) {
                return new ImageSearchMatch(
                        similarProducts.stream().map(ScoredProduct::product).toList(),
                        true,
                        35
                );
            }
            if (!sameCategoryProducts.isEmpty()) {
                return new ImageSearchMatch(
                        sameCategoryProducts.stream()
                                .limit(Math.max(1, limit))
                                .map(ScoredProduct::product)
                                .toList(),
                        true,
                        0
                );
            }
        }

        List<ScoredProduct> globalSimilar = deduped.stream()
                .map(product -> new ScoredProduct(product, scoreProductForImage(product, insight)))
                .filter(scored -> scored.score() >= 32)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .limit(Math.max(1, limit))
                .toList();
        if (!globalSimilar.isEmpty()) {
            return new ImageSearchMatch(
                    globalSimilar.stream().map(ScoredProduct::product).toList(),
                    true,
                    32
            );
        }

        List<ScoredProduct> broadFallback = deduped.stream()
                .map(product -> new ScoredProduct(product, scoreProductForImage(product, insight)))
                .filter(scored -> scored.score() > 0)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .limit(Math.max(1, limit))
                .toList();
        if (!broadFallback.isEmpty()) {
            return new ImageSearchMatch(
                    broadFallback.stream().map(ScoredProduct::product).toList(),
                    true,
                    0
            );
        }

        return new ImageSearchMatch(getTrendingProducts(limit), true, 0);
    }

    public ShoppingIntent toImageShoppingIntent(ImageInsight insight) {
        String tag = insight.tags().isEmpty() ? null : insight.tags().get(0);
        String query = emptyToNull(insight.extractedHint());
        if (query == null) {
            query = tightenQuery(String.join(" ", insight.colors()), insight.category(), insight.brand(), tag);
        }
        return new ShoppingIntent(query == null ? "" : query, insight.category(), insight.brand(), tag, null, null, true, "relevance", List.of("Visual search"));
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
        if (text == null || text.isBlank()) {
            return null;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String spaced = value.replace('-', ' ');
            String singular = value.endsWith("s") ? value.substring(0, value.length() - 1) : value;
            String singularSpaced = singular.replace('-', ' ');
            if (text.contains(value) || text.contains(spaced) || text.contains(singular) || text.contains(singularSpaced)) {
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
                .replaceAll("between\\s+(?:rs\\.?\\s*)?" + PRICE_TOKEN + "\\s+(?:and|to)\\s+(?:rs\\.?\\s*)?" + PRICE_TOKEN, " ")
                .replaceAll("(?:under|below|less than|max(?:imum)? of|above|over|more than|starting at|from)\\s+(?:rs\\.?\\s*)?" + PRICE_TOKEN, " ")
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
            String normalized = raw.replace(",", "").trim().toLowerCase(Locale.ROOT);
            double multiplier = 1.0;
            if (normalized.endsWith("lakhs")) {
                normalized = normalized.substring(0, normalized.length() - "lakhs".length()).trim();
                multiplier = 100000.0;
            } else if (normalized.endsWith("lakh") || normalized.endsWith("lac")) {
                normalized = normalized.replaceFirst("\\s*(lakh|lac)$", "").trim();
                multiplier = 100000.0;
            } else if (normalized.endsWith("thousand")) {
                normalized = normalized.substring(0, normalized.length() - "thousand".length()).trim();
                multiplier = 1000.0;
            } else if (normalized.endsWith("k")) {
                normalized = normalized.substring(0, normalized.length() - 1).trim();
                multiplier = 1000.0;
            }
            double rupees = Double.parseDouble(normalized) * multiplier;
            return (int) Math.round(rupees * 100.0);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ShoppingIntent relaxShoppingIntent(String rawText, ShoppingIntent intent) {
        String fallbackQuery = tightenQuery(normalize(rawText), intent.category(), intent.brand(), intent.tag());
        if (intent.maxPrice() != null || intent.minPrice() != null) {
            return new ShoppingIntent(
                    emptyToNull(fallbackQuery) == null ? intent.query() : fallbackQuery,
                    intent.category(),
                    intent.brand(),
                    intent.tag(),
                    null,
                    null,
                    true,
                    "price_asc",
                    appendSignal(intent.matchedSignals(), "Relaxed the budget to show closest matches")
            );
        }
        if (hasText(intent.query()) && fallbackQuery != null && !fallbackQuery.equals(intent.query())) {
            return new ShoppingIntent(
                    fallbackQuery,
                    intent.category(),
                    intent.brand(),
                    intent.tag(),
                    intent.minPrice(),
                    intent.maxPrice(),
                    intent.inStock(),
                    intent.sort(),
                    appendSignal(intent.matchedSignals(), "Tightened the search wording for stronger matches")
            );
        }
        return intent;
    }

    private String summarizeRelaxedSearch(ShoppingIntent strict, ShoppingIntent effective, int resultsCount) {
        boolean budgetChanged = !Objects.equals(strict.minPrice(), effective.minPrice())
                || !Objects.equals(strict.maxPrice(), effective.maxPrice());
        if (budgetChanged) {
            return "I could not find an exact match in that budget, so I am showing the closest live options instead.";
        }
        if (!Objects.equals(strict.query(), effective.query())) {
            return "I tightened the search intent and found " + resultsCount + " closer live catalog matches.";
        }
        return "Found " + resultsCount + " relevant live catalog matches.";
    }

    private List<String> appendSignal(List<String> source, String addition) {
        List<String> values = new ArrayList<>(source == null ? List.of() : source);
        if (addition != null && !addition.isBlank() && !values.contains(addition)) {
            values.add(addition);
        }
        return values;
    }

    private String mergeKnownValue(String primary, String secondary, List<String> knownValues) {
        String normalizedPrimary = normalize(primary);
        if (normalizedPrimary != null && knownValues.contains(normalizedPrimary)) {
            return normalizedPrimary;
        }
        String normalizedSecondary = normalize(secondary);
        if (normalizedSecondary == null) {
            return normalizedPrimary;
        }
        for (String knownValue : knownValues) {
            String spaced = knownValue.replace('-', ' ');
            if (normalizedSecondary.equals(knownValue)
                    || normalizedSecondary.equals(spaced)
                    || normalizedSecondary.contains(knownValue)
                    || normalizedSecondary.contains(spaced)) {
                return knownValue;
            }
        }
        return normalizedPrimary;
    }

    private String tightenQuery(String text, String category, String brand, String tag) {
        String cleaned = stripNoise(text == null ? "" : text, category, brand, tag);
        List<String> tokens = Arrays.stream(cleaned.split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() > 2)
                .filter(token -> !TEXT_STOP_WORDS.contains(token))
                .distinct()
                .limit(4)
                .toList();
        return tokens.isEmpty() ? null : String.join(" ", tokens);
    }

    private String joinHints(String primary, String secondary) {
        String left = emptyToNull(primary);
        String right = emptyToNull(secondary);
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (left.contains(right) || right.contains(left)) {
            return left.length() >= right.length() ? left : right;
        }
        return (left + " " + right).trim();
    }

    private int scoreProductForImage(ProductDto.ProductCard product, ImageInsight insight) {
        int metadataScore = scoreImageMatch(product, insight);
        int visualScore = scoreVisualSimilarity(product, insight.visualSignature());
        if (metadataScore > 0 && visualScore > 0) {
            return Math.min(100, ((metadataScore + visualScore) / 2) + 12);
        }
        return Math.max(metadataScore, visualScore);
    }

    private int scoreImageMatch(ProductDto.ProductCard product, ImageInsight insight) {
        int score = 0;
        String haystack = normalizeImageText(
                product.title(),
                product.description(),
                product.categoryName(),
                product.brandName(),
                product.variantName(),
                product.sku(),
                String.join(" ", product.tags())
        );

        if (insight.category() != null && matchesField(product.categorySlug(), insight.category(), product.categoryName())) {
            score += 32;
        }
        if (insight.brand() != null && matchesField(product.brandSlug(), insight.brand(), product.brandName())) {
            score += 24;
        }
        for (String tag : insight.tags()) {
            if (haystack.contains(tag.replace('-', ' ')) || haystack.contains(tag)) {
                score += 12;
            }
        }
        for (String token : tokenizeQuery(insight.extractedHint())) {
            if (haystack.contains(token)) {
                score += 8;
            }
        }
        for (String color : insight.colors()) {
            if (haystack.contains(color)) {
                score += 6;
            }
        }

        if (insight.extractedHint() != null && !insight.extractedHint().isBlank()
                && normalize(product.title()).contains(normalize(insight.extractedHint()))) {
            score += 18;
        }

        return Math.min(score, 100);
    }

    private int scoreVisualSimilarity(ProductDto.ProductCard product, VisualSignature uploadedSignature) {
        if (product == null || uploadedSignature == null) {
            return 0;
        }
        int best = 0;
        for (String url : candidateImageUrls(product)) {
            Optional<VisualSignature> signature = loadRemoteVisualSignature(url);
            if (signature.isEmpty()) {
                continue;
            }
            best = Math.max(best, compareVisualSignatures(uploadedSignature, signature.get()));
            if (best >= 95) {
                break;
            }
        }
        return best;
    }

    private List<String> candidateImageUrls(ProductDto.ProductCard product) {
        List<String> urls = new ArrayList<>();
        if (hasText(product.coverImage())) {
            urls.add(product.coverImage().trim());
        }
        if (product.media() == null) {
            return urls;
        }
        for (ProductDto.Media media : product.media()) {
            if (media == null || !hasText(media.url())) {
                continue;
            }
            String url = media.url().trim();
            if (!urls.contains(url)) {
                urls.add(url);
            }
            if (urls.size() >= 3) {
                break;
            }
        }
        return urls;
    }

    private Optional<VisualSignature> loadRemoteVisualSignature(String imageUrl) {
        if (!hasText(imageUrl)) {
            return Optional.empty();
        }
        return remoteImageSignatureCache.computeIfAbsent(imageUrl.trim(), this::downloadRemoteVisualSignature);
    }

    private Optional<VisualSignature> downloadRemoteVisualSignature(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(java.time.Duration.ofSeconds(8))
                    .header("User-Agent", "NexBuy-AI-ImageSearch/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().length == 0) {
                return Optional.empty();
            }
            return Optional.ofNullable(buildVisualSignature(readImage(response.body())));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private int compareVisualSignatures(VisualSignature left, VisualSignature right) {
        if (left == null || right == null) {
            return 0;
        }

        double hashSimilarity = 1.0 - (Long.bitCount(left.differenceHash() ^ right.differenceHash()) / 64.0);
        double paletteSimilarity = overlapRatio(left.colorNames(), right.colorNames());
        double brightnessSimilarity = 1.0 - Math.min(1.0, Math.abs(left.brightRatio() - right.brightRatio()));
        double colorfulSimilarity = 1.0 - Math.min(1.0, Math.abs(left.colorfulRatio() - right.colorfulRatio()));
        double centerSimilarity = 1.0 - Math.min(1.0, Math.abs(left.centerBandRatio() - right.centerBandRatio()));
        double aspectSimilarity = 1.0 - Math.min(1.0, Math.abs(left.aspectRatio() - right.aspectRatio()) / 2.0);
        double phoneSimilarity = 1.0 - Math.min(1.0, Math.abs(left.phoneAffinity() - right.phoneAffinity()));

        double score = (hashSimilarity * 34)
                + (paletteSimilarity * 12)
                + (brightnessSimilarity * 10)
                + (colorfulSimilarity * 10)
                + (centerSimilarity * 14)
                + (aspectSimilarity * 8)
                + (phoneSimilarity * 12);

        if (left.suggestedCategory() != null && Objects.equals(left.suggestedCategory(), right.suggestedCategory())) {
            score += 10;
        }
        return (int) Math.round(Math.max(0, Math.min(100, score)));
    }

    private double overlapRatio(List<String> left, List<String> right) {
        if (left == null || left.isEmpty() || right == null || right.isEmpty()) {
            return 0;
        }
        List<String> uniqueLeft = left.stream().filter(this::hasText).distinct().toList();
        List<String> uniqueRight = right.stream().filter(this::hasText).distinct().toList();
        if (uniqueLeft.isEmpty() || uniqueRight.isEmpty()) {
            return 0;
        }
        long overlap = uniqueLeft.stream().filter(uniqueRight::contains).count();
        return overlap / (double) Math.max(uniqueLeft.size(), uniqueRight.size());
    }

    private boolean matchesField(String slug, String expected, String label) {
        String normalizedExpected = normalize(expected);
        return Objects.equals(normalize(slug), normalizedExpected)
                || Objects.equals(normalize(label), normalizedExpected)
                || (normalize(label) != null && normalize(label).contains(normalizedExpected))
                || (normalize(slug) != null && normalize(slug).contains(normalizedExpected));
    }

    private List<String> tokenizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() > 2)
                .filter(token -> !TEXT_STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private String normalizeImageText(String... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT).replace('-', ' '))
                .collect(Collectors.joining(" "));
    }

    private String hexToColorName(String hex) {
        String normalized = normalize(hex);
        if (normalized == null || !normalized.startsWith("#") || normalized.length() != 7) {
            return "neutral";
        }
        int red = Integer.parseInt(normalized.substring(1, 3), 16);
        int green = Integer.parseInt(normalized.substring(3, 5), 16);
        int blue = Integer.parseInt(normalized.substring(5, 7), 16);
        if (red > 180 && green > 180 && blue > 180) {
            return "white";
        }
        if (red < 70 && green < 70 && blue < 70) {
            return "black";
        }
        if (red > blue + 40 && red > green + 20) {
            return "red";
        }
        if (blue > red + 30 && blue > green) {
            return "blue";
        }
        if (green > red + 20 && green > blue) {
            return "green";
        }
        if (red > 170 && green > 120 && blue < 120) {
            return "gold";
        }
        if (red > 120 && blue > 120 && green < 120) {
            return "purple";
        }
        return "neutral";
    }

    private String normalizeSort(String sort) {
        String normalized = normalize(sort);
        if (normalized == null) {
            return "relevance";
        }
        return switch (normalized) {
            case "relevance", "price_asc", "price_desc", "newest", "title", "stock_desc" -> normalized;
            default -> "relevance";
        };
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean containsAny(String text, String... values) {
        return Arrays.stream(values).anyMatch(text::contains);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private BufferedImage readImage(byte[] fileBytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(fileBytes));
        } catch (IOException ex) {
            return null;
        }
    }

    private VisualSignature buildVisualSignature(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        double aspectRatio = height / (double) width;
        double brightRatio = brightPixelRatio(image);
        double colorfulRatio = colorfulPixelRatio(image, 0, width, 0, height);
        double centerBandRatio = colorfulPixelRatio(image,
                (int) (width * 0.32),
                (int) (width * 0.68),
                (int) (height * 0.12),
                (int) (height * 0.94));
        double leftBandRatio = colorfulPixelRatio(image,
                0,
                (int) (width * 0.28),
                (int) (height * 0.12),
                (int) (height * 0.94));
        double rightBandRatio = colorfulPixelRatio(image,
                (int) (width * 0.72),
                width,
                (int) (height * 0.12),
                (int) (height * 0.94));
        double edgeColorRatio = (leftBandRatio + rightBandRatio) / 2.0;
        double phoneAffinity = calculatePhoneAffinity(aspectRatio, brightRatio, colorfulRatio, centerBandRatio, edgeColorRatio);
        List<String> colorNames = extractPalette(image).stream()
                .map(this::hexToColorName)
                .filter(this::hasText)
                .distinct()
                .toList();

        return new VisualSignature(
                computeDifferenceHash(image),
                aspectRatio,
                brightRatio,
                colorfulRatio,
                centerBandRatio,
                edgeColorRatio,
                phoneAffinity,
                colorNames,
                phoneAffinity >= 0.48 ? "mobiles" : null
        );
    }

    private List<String> extractPalette(BufferedImage image) {
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
    }

    private String inferVisualCategory(BufferedImage image) {
        VisualSignature signature = buildVisualSignature(image);
        return signature == null ? null : signature.suggestedCategory();
    }

    private List<String> inferVisualTags(String category, BufferedImage image, List<String> knownTags) {
        if (!"mobiles".equals(category)) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        VisualSignature signature = buildVisualSignature(image);
        if (knownTags.contains("camera")) {
            tags.add("camera");
        }
        if (signature != null && signature.phoneAffinity() > 0.58 && knownTags.contains("5g")) {
            tags.add("5g");
        }
        if (signature != null && signature.centerBandRatio() > 0.14 && knownTags.contains("premium")) {
            tags.add("premium");
        }
        return tags;
    }

    private String inferCatalogCategory(VisualSignature uploadedSignature, List<String> categories) {
        if (uploadedSignature == null || categories == null || categories.isEmpty()) {
            return null;
        }

        String bestCategory = null;
        double bestScore = 0;
        for (String category : categories) {
            List<ProductDto.ProductCard> representatives = productService.getCatalog(
                    null,
                    category,
                    null,
                    null,
                    null,
                    null,
                    true,
                    "newest",
                    1,
                    6
            ).items();

            List<Integer> scores = representatives.stream()
                    .map(product -> scoreVisualSimilarity(product, uploadedSignature))
                    .filter(score -> score > 0)
                    .sorted(Collections.reverseOrder())
                    .limit(2)
                    .toList();
            if (scores.isEmpty()) {
                continue;
            }
            double categoryScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            if (categoryScore > bestScore) {
                bestScore = categoryScore;
                bestCategory = category;
            }
        }

        return bestScore >= 34 ? bestCategory : null;
    }

    private double colorfulPixelRatio(BufferedImage image, int startX, int endX, int startY, int endY) {
        if (image == null) {
            return 0;
        }
        int clampedStartX = Math.max(0, startX);
        int clampedEndX = Math.min(image.getWidth(), endX);
        int clampedStartY = Math.max(0, startY);
        int clampedEndY = Math.min(image.getHeight(), endY);
        if (clampedStartX >= clampedEndX || clampedStartY >= clampedEndY) {
            return 0;
        }

        int stepX = Math.max(1, (clampedEndX - clampedStartX) / 80);
        int stepY = Math.max(1, (clampedEndY - clampedStartY) / 80);
        int colorful = 0;
        int total = 0;
        for (int y = clampedStartY; y < clampedEndY; y += stepY) {
            for (int x = clampedStartX; x < clampedEndX; x += stepX) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
                total++;
                if (hsb[1] > 0.18f && hsb[2] > 0.22f) {
                    colorful++;
                }
            }
        }
        return total == 0 ? 0 : colorful / (double) total;
    }

    private double brightPixelRatio(BufferedImage image) {
        if (image == null) {
            return 0;
        }
        int stepX = Math.max(1, image.getWidth() / 120);
        int stepY = Math.max(1, image.getHeight() / 120);
        int bright = 0;
        int total = 0;
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
                total++;
                if (hsb[2] > 0.9f && hsb[1] < 0.12f) {
                    bright++;
                }
            }
        }
        return total == 0 ? 0 : bright / (double) total;
    }

    private double calculatePhoneAffinity(double aspectRatio,
                                          double brightRatio,
                                          double colorfulRatio,
                                          double centerBandRatio,
                                          double edgeColorRatio) {
        double aspectScore = aspectRatio > 1.1 && aspectRatio < 2.5 ? 0.22 : 0.0;
        double brightScore = clamp01((brightRatio - 0.28) / 0.45) * 0.23;
        double colorfulScore = clamp01((colorfulRatio - 0.03) / 0.32) * 0.15;
        double centerScore = clamp01((centerBandRatio - 0.05) / 0.35) * 0.24;
        double dominanceScore = clamp01((centerBandRatio - edgeColorRatio) / 0.25) * 0.16;
        return clamp01(aspectScore + brightScore + colorfulScore + centerScore + dominanceScore);
    }

    private double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private long computeDifferenceHash(BufferedImage image) {
        BufferedImage scaled = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, 9, 8, null);
        } finally {
            graphics.dispose();
        }

        long hash = 0L;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int left = scaled.getRaster().getSample(x, y, 0);
                int right = scaled.getRaster().getSample(x + 1, y, 0);
                hash <<= 1;
                if (left > right) {
                    hash |= 1L;
                }
            }
        }
        return hash;
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
        public ShoppingIntent {
            query = query == null ? "" : query.trim();
            sort = sort == null || sort.isBlank() ? "relevance" : sort;
            matchedSignals = matchedSignals == null ? List.of() : List.copyOf(matchedSignals);
        }
    }

    public record RecommendationBundle(boolean personalized,
                                       String headline,
                                       String summary,
                                       List<String> signals,
                                       List<ProductDto.ProductCard> products) {
    }

    public record ImageInsight(String extractedHint,
                               String category,
                               String brand,
                               List<String> tags,
                               List<String> colors,
                               List<String> palette,
                               String confidence,
                               String fileName,
                               VisualSignature visualSignature) {
        public ImageInsight {
            tags = tags == null ? List.of() : List.copyOf(tags);
            colors = colors == null ? List.of() : List.copyOf(colors);
            palette = palette == null ? List.of() : List.copyOf(palette);
        }
    }

    public record SearchPlan(String rawText,
                             ShoppingIntent interpretedIntent,
                             ShoppingIntent effectiveIntent,
                             List<ProductDto.ProductCard> products,
                             boolean relaxed,
                             String summary) {
        public SearchPlan {
            products = products == null ? List.of() : List.copyOf(products);
            summary = summary == null ? "" : summary;
        }
    }

    private record ScoredProduct(ProductDto.ProductCard product, int score) {
    }

    public record VisualSignature(long differenceHash,
                                  double aspectRatio,
                                  double brightRatio,
                                  double colorfulRatio,
                                  double centerBandRatio,
                                  double edgeColorRatio,
                                  double phoneAffinity,
                                  List<String> colorNames,
                                  String suggestedCategory) {
        public VisualSignature {
            colorNames = colorNames == null ? List.of() : List.copyOf(colorNames);
        }
    }

    public record ImageSearchMatch(List<ProductDto.ProductCard> products,
                                   boolean similarFallback,
                                   int appliedThreshold) {
        public ImageSearchMatch {
            products = products == null ? List.of() : List.copyOf(products);
        }
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
