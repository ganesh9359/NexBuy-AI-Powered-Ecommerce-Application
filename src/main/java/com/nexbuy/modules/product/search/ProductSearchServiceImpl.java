package com.nexbuy.modules.product.search;

import com.nexbuy.modules.product.dto.ProductDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductSearchServiceImpl implements ProductSearchService {

    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, List<String>> SEARCH_MAPPINGS = Map.of(
        "phone", List.of("mobile", "smartphone", "cell", "iphone", "android", "samsung", "oneplus"),
        "mobile", List.of("phone", "smartphone", "cell", "iphone", "android", "samsung", "oneplus"),
        "laptop", List.of("computer", "notebook", "pc", "macbook", "dell", "hp", "lenovo"),
        "computer", List.of("laptop", "pc", "desktop", "notebook", "macbook", "dell", "hp"),
        "headphone", List.of("earphone", "audio", "music", "sound", "beats", "sony", "jbl"),
        "watch", List.of("smartwatch", "time", "wearable", "apple", "samsung", "fitbit"),
        "camera", List.of("photo", "picture", "lens", "photography", "canon", "nikon", "sony"),
        "gaming", List.of("game", "console", "controller", "xbox", "playstation", "nintendo")
    );

    public ProductSearchServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProductDto.ProductListResponse enhancedSearch(String query, String category, String brand,
                                                         String tag, Integer minPrice, Integer maxPrice,
                                                         Boolean inStock, String sort, int page, int size) {
        return new ProductDto.ProductListResponse(
            Collections.emptyList(), page, size, 0, 0,
            query, category, brand, tag, sort != null ? sort : "relevance", null
        );
    }

    @Override
    public List<String> getSearchSuggestions(String partialQuery, int limit) {
        if (partialQuery == null || partialQuery.trim().isEmpty()) {
            return getTrendingSuggestions(limit);
        }

        String normalizedQuery = partialQuery.toLowerCase().trim();
        Set<String> suggestions = new LinkedHashSet<>();

        try {
            List<String> titleSuggestions = jdbcTemplate.queryForList(
                "SELECT DISTINCT p.title FROM products p WHERE LOWER(p.title) LIKE ? AND LOWER(p.status) = 'active' ORDER BY p.title LIMIT ?",
                String.class, "%" + normalizedQuery + "%", limit);
            suggestions.addAll(titleSuggestions);

            if (suggestions.size() < limit) {
                List<String> brandSuggestions = jdbcTemplate.queryForList(
                    "SELECT DISTINCT b.name FROM brands b JOIN products p ON p.brand_id = b.id WHERE LOWER(b.name) LIKE ? AND LOWER(p.status) = 'active' ORDER BY b.name LIMIT ?",
                    String.class, "%" + normalizedQuery + "%", limit - suggestions.size());
                suggestions.addAll(brandSuggestions);
            }

            if (suggestions.size() < limit) {
                List<String> categorySuggestions = jdbcTemplate.queryForList(
                    "SELECT DISTINCT c.name FROM categories c JOIN products p ON p.category_id = c.id WHERE LOWER(c.name) LIKE ? AND LOWER(p.status) = 'active' ORDER BY c.name LIMIT ?",
                    String.class, "%" + normalizedQuery + "%", limit - suggestions.size());
                suggestions.addAll(categorySuggestions);
            }

            for (Map.Entry<String, List<String>> entry : SEARCH_MAPPINGS.entrySet()) {
                if (normalizedQuery.contains(entry.getKey()) || entry.getValue().stream().anyMatch(normalizedQuery::contains)) {
                    entry.getValue().stream()
                        .filter(term -> !term.equals(normalizedQuery))
                        .limit(3)
                        .forEach(suggestions::add);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting search suggestions: " + e.getMessage());
        }

        return suggestions.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<ProductDto.ProductCard> getRecommendedProducts(String originalQuery, int limit) {
        try {
            String sql = """
                SELECT p.id
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                LEFT JOIN product_variants pv ON pv.id = (
                    SELECT pv2.id FROM product_variants pv2 WHERE pv2.product_id = p.id ORDER BY pv2.id ASC LIMIT 1
                )
                LEFT JOIN inventory i ON i.variant_id = pv.id
                WHERE LOWER(p.status) = 'active'
                ORDER BY COALESCE(i.stock_qty, 0) DESC, p.created_at DESC
                LIMIT ?
                """;
            List<Long> productIds = jdbcTemplate.queryForList(sql, Long.class, limit);
            return loadProductCards(productIds);
        } catch (Exception e) {
            System.err.println("Error getting recommended products: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProductDto.ProductCard> fuzzySearch(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.toLowerCase().trim();
        String fuzzyQuery = "%" + normalizedQuery.replaceAll("\\s+", "%") + "%";

        try {
            String sql = """
                SELECT DISTINCT p.id
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                WHERE LOWER(p.status) = 'active'
                  AND (
                      LOWER(p.title) LIKE ?
                      OR LOWER(COALESCE(p.description, '')) LIKE ?
                      OR LOWER(COALESCE(b.name, '')) LIKE ?
                      OR LOWER(c.name) LIKE ?
                  )
                ORDER BY p.created_at DESC
                LIMIT ?
                """;
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class,
                fuzzyQuery, fuzzyQuery, fuzzyQuery, fuzzyQuery, limit);
            return loadProductCards(ids);
        } catch (Exception e) {
            System.err.println("Error in fuzzy search: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProductDto.ProductCard> findSimilarProducts(Long productId, int limit) {
        try {
            String sql = """
                SELECT DISTINCT p.id
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN product_variants pv ON pv.id = (
                    SELECT pv2.id FROM product_variants pv2 WHERE pv2.product_id = p.id ORDER BY pv2.id ASC LIMIT 1
                )
                WHERE p.id != ? AND LOWER(p.status) = 'active'
                  AND p.category_id = (SELECT category_id FROM products WHERE id = ?)
                ORDER BY p.created_at DESC
                LIMIT ?
                """;
            List<Long> similarIds = jdbcTemplate.queryForList(sql, Long.class, productId, productId, limit);
            return loadProductCards(similarIds);
        } catch (Exception e) {
            System.err.println("Error finding similar products: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> getTrendingSuggestions(int limit) {
        try {
            return jdbcTemplate.queryForList(
                "SELECT DISTINCT p.title FROM products p WHERE LOWER(p.status) = 'active' ORDER BY p.created_at DESC LIMIT ?",
                String.class, limit);
        } catch (Exception e) {
            return List.of("Smartphones", "Laptops", "Headphones", "Watches", "Cameras");
        }
    }

    private List<ProductDto.ProductCard> loadProductCards(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String inClause = productIds.stream().map(id -> "?").collect(Collectors.joining(", "));
            String sql = ("""
                SELECT p.id, p.slug, p.title, p.description, p.cover_image,
                       c.name as category_name, c.slug as category_slug,
                       b.name as brand_name, b.slug as brand_slug,
                       pv.sku, pv.name as variant_name,
                       COALESCE(pv.price_cents, 0) as price_cents,
                       pv.compare_at_cents, pv.currency,
                       COALESCE(i.stock_qty, 0) as stock_qty,
                       COALESCE(i.low_stock_threshold, 5) as low_stock_threshold,
                       COALESCE(i.is_backorder_allowed, false) as is_backorder_allowed
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                LEFT JOIN product_variants pv ON pv.id = (
                    SELECT pv2.id FROM product_variants pv2 WHERE pv2.product_id = p.id ORDER BY pv2.id ASC LIMIT 1
                )
                LEFT JOIN inventory i ON i.variant_id = pv.id
                WHERE p.id IN (%s)
                """).formatted(inClause);
            return jdbcTemplate.query(sql, (rs, rowNum) -> new ProductDto.ProductCard(
                rs.getLong("id"), rs.getString("slug"), rs.getString("title"),
                rs.getString("description"), rs.getString("cover_image"),
                rs.getString("category_name"), rs.getString("category_slug"),
                rs.getString("brand_name"), rs.getString("brand_slug"),
                rs.getString("sku"), rs.getString("variant_name"),
                rs.getInt("price_cents"), rs.getObject("compare_at_cents", Integer.class),
                rs.getString("currency") == null ? "INR" : rs.getString("currency"),
                null, Collections.emptyList(), Collections.emptyList()
            ), productIds.toArray());
        } catch (Exception e) {
            System.err.println("Error loading product cards: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
