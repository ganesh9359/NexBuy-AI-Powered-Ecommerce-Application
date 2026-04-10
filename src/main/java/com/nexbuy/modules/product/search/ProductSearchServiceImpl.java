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
    
    public ProductSearchServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
        "phone", List.of("mobile", "smartphone", "cell", "iphone", "android", "samsung", "oneplus"),
        "mobile", List.of("phone", "smartphone", "cell", "iphone", "android", "samsung", "oneplus"),
        "laptop", List.of("computer", "notebook", "pc", "macbook", "dell", "hp", "lenovo"),
        "computer", List.of("laptop", "pc", "desktop", "notebook", "macbook", "dell", "hp"),
        "headphone", List.of("earphone", "audio", "music", "sound", "beats", "sony", "jbl"),
        "earphone", List.of("headphone", "audio", "music", "sound", "beats", "sony", "jbl"),
        "watch", List.of("smartwatch", "time", "wearable", "apple", "samsung", "fitbit"),
        "camera", List.of("photo", "picture", "lens", "photography", "canon", "nikon", "sony"),
        "gaming", List.of("game", "console", "controller", "xbox", "playstation", "nintendo"),
        "tv", List.of("television", "smart tv", "led", "oled", "samsung", "lg", "sony"),
        "tablet", List.of("ipad", "android tablet", "samsung tab", "lenovo tab"),
        "speaker", List.of("bluetooth speaker", "wireless speaker", "jbl", "bose", "sony"),
        "charger", List.of("power bank", "charging cable", "wireless charger", "fast charger"),
        "case", List.of("cover", "protection", "phone case", "laptop bag", "screen guard")
    );

    // Search term mappings for intelligent matching
    private static final Map<String, List<String>> SEARCH_MAPPINGS = Map.of(

    @Override
    public ProductDto.ProductListResponse enhancedSearch(String query, String category, String brand, 
                                                        String tag, Integer minPrice, Integer maxPrice, 
                                                        Boolean inStock, String sort, int page, int size) {
        
        // First try regular search
        ProductDto.ProductListResponse response = productService.search(query, category, brand, tag, 
                                                                       minPrice, maxPrice, inStock, sort, page, size);
        
        // If no results and we have a query, try enhanced search
        if (response.items().isEmpty() && query != null && !query.trim().isEmpty()) {
            List<ProductDto.ProductCard> enhancedResults = performEnhancedSearch(query.trim(), size);
            
            if (!enhancedResults.isEmpty()) {
                return new ProductDto.ProductListResponse(
                    enhancedResults,
                    1,
                    size,
                    enhancedResults.size(),
                    1,
                    query,
                    category,
                    brand,
                    tag,
                    sort != null ? sort : "relevance",
                    response.filters()
                );
            }
            
            // If still no results, show recommended products
            List<ProductDto.ProductCard> recommendations = getRecommendedProducts(query, size);
            if (!recommendations.isEmpty()) {
                return new ProductDto.ProductListResponse(
                    recommendations,
                    1,
                    size,
                    recommendations.size(),
                    1,
                    query,
                    category,
                    brand,
                    tag,
                    sort != null ? sort : "relevance",
                    response.filters()
                );
            }
        }
        
        return response;
    }

    @Override
    public List<String> getSearchSuggestions(String partialQuery, int limit) {
        if (partialQuery == null || partialQuery.trim().isEmpty()) {
            return getTrendingSuggestions(limit);
        }
        
        String normalizedQuery = partialQuery.toLowerCase().trim();
        Set<String> suggestions = new LinkedHashSet<>();
        
        try {
            // Get suggestions from product titles
            String sql = """
                SELECT DISTINCT p.title
                FROM products p
                WHERE LOWER(p.title) LIKE ? AND LOWER(p.status) = 'active'
                ORDER BY p.title
                LIMIT ?
                """;
            
            List<String> titleSuggestions = jdbcTemplate.queryForList(sql, String.class, 
                                                                     "%" + normalizedQuery + "%", limit);
            suggestions.addAll(titleSuggestions);
            
            // Get suggestions from brands
            if (suggestions.size() < limit) {
                String brandSql = """
                    SELECT DISTINCT b.name
                    FROM brands b
                    JOIN products p ON p.brand_id = b.id
                    WHERE LOWER(b.name) LIKE ? AND LOWER(p.status) = 'active'
                    ORDER BY b.name
                    LIMIT ?
                    """;
                
                List<String> brandSuggestions = jdbcTemplate.queryForList(brandSql, String.class, 
                                                                         "%" + normalizedQuery + "%", limit - suggestions.size());
                suggestions.addAll(brandSuggestions);
            }
            
            // Get suggestions from categories
            if (suggestions.size() < limit) {
                String categorySql = """
                    SELECT DISTINCT c.name
                    FROM categories c
                    JOIN products p ON p.category_id = c.id
                    WHERE LOWER(c.name) LIKE ? AND LOWER(p.status) = 'active'
                    ORDER BY c.name
                    LIMIT ?
                    """;
                
                List<String> categorySuggestions = jdbcTemplate.queryForList(categorySql, String.class, 
                                                                            "%" + normalizedQuery + "%", limit - suggestions.size());
                suggestions.addAll(categorySuggestions);
            }
            
            // Add intelligent suggestions based on mappings
            for (Map.Entry<String, List<String>> entry : SEARCH_MAPPINGS.entrySet()) {
                if (normalizedQuery.contains(entry.getKey()) || entry.getValue().stream().anyMatch(normalizedQuery::contains)) {
                    suggestions.addAll(entry.getValue().stream()
                                     .filter(term -> !term.equals(normalizedQuery))
                                     .limit(3)
                                     .collect(Collectors.toList()));
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
            // Get popular products with good stock
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
        Set<Long> productIds = new LinkedHashSet<>();
        
        try {
            // Fuzzy search with SOUNDEX and LEVENSHTEIN-like matching
            String fuzzyQuery = "%" + normalizedQuery.replaceAll("\\s+", "%") + "%";
            
            String sql = """
                SELECT DISTINCT p.id,
                       CASE 
                           WHEN LOWER(p.title) = ? THEN 1
                           WHEN LOWER(p.title) LIKE ? THEN 2
                           WHEN LOWER(COALESCE(b.name, '')) LIKE ? THEN 3
                           WHEN LOWER(COALESCE(p.description, '')) LIKE ? THEN 4
                           ELSE 5
                       END as relevance_score
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
                ORDER BY relevance_score ASC, p.created_at DESC
                LIMIT ?
                """;
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, 
                normalizedQuery, normalizedQuery + "%", fuzzyQuery, fuzzyQuery,
                fuzzyQuery, fuzzyQuery, fuzzyQuery, fuzzyQuery, limit);
            
            productIds.addAll(results.stream()
                            .map(row -> ((Number) row.get("id")).longValue())
                            .collect(Collectors.toList()));
            
        } catch (Exception e) {
            System.err.println("Error in fuzzy search: " + e.getMessage());
        }
        
        return loadProductCards(new ArrayList<>(productIds));
    }

    @Override
    public List<ProductDto.ProductCard> findSimilarProducts(Long productId, int limit) {
        try {
            // Find similar products based on category, brand, price range, and tags
            String sql = """
                WITH target_product AS (
                    SELECT p.category_id, p.brand_id, pv.price_cents,
                           ARRAY_AGG(pt.tag) as tags
                    FROM products p
                    LEFT JOIN product_variants pv ON pv.product_id = p.id
                    LEFT JOIN product_tags pt ON pt.product_id = p.id
                    WHERE p.id = ?
                    GROUP BY p.category_id, p.brand_id, pv.price_cents
                )
                SELECT DISTINCT p.id,
                       CASE 
                           WHEN p.category_id = tp.category_id AND p.brand_id = tp.brand_id THEN 1
                           WHEN p.category_id = tp.category_id THEN 2
                           WHEN p.brand_id = tp.brand_id THEN 3
                           ELSE 4
                       END as similarity_score
                FROM products p
                JOIN target_product tp ON 1=1
                LEFT JOIN product_variants pv ON pv.product_id = p.id
                WHERE p.id != ? 
                  AND LOWER(p.status) = 'active'
                  AND ABS(COALESCE(pv.price_cents, 0) - COALESCE(tp.price_cents, 0)) <= (COALESCE(tp.price_cents, 0) * 0.5)
                ORDER BY similarity_score ASC, ABS(COALESCE(pv.price_cents, 0) - COALESCE(tp.price_cents, 0)) ASC
                LIMIT ?
                """;
            
            List<Long> similarIds = jdbcTemplate.queryForList(sql, Long.class, productId, productId, limit);
            return loadProductCards(similarIds);
            
        } catch (Exception e) {
            System.err.println("Error finding similar products: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ProductDto.ProductCard> performEnhancedSearch(String query, int limit) {
        String normalizedQuery = query.toLowerCase().trim();
        Set<Long> productIds = new LinkedHashSet<>();
        
        // Try exact matches first
        productIds.addAll(searchByExactMatch(normalizedQuery, limit / 4));
        
        // Try fuzzy search
        if (productIds.size() < limit) {
            List<ProductDto.ProductCard> fuzzyResults = fuzzySearch(query, limit - productIds.size());
            productIds.addAll(fuzzyResults.stream().map(p -> p.id()).collect(Collectors.toList()));
        }
        
        // Try intelligent mapping search
        if (productIds.size() < limit) {
            productIds.addAll(searchWithMappings(normalizedQuery, limit - productIds.size()));
        }
        
        return loadProductCards(new ArrayList<>(productIds));
    }

    private List<Long> searchByExactMatch(String query, int limit) {
        try {
            String sql = """
                SELECT DISTINCT p.id
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                WHERE LOWER(p.status) = 'active'
                  AND (
                      LOWER(p.title) = ?
                      OR LOWER(COALESCE(b.name, '')) = ?
                      OR LOWER(c.name) = ?
                  )
                ORDER BY p.created_at DESC
                LIMIT ?
                """;
            
            return jdbcTemplate.queryForList(sql, Long.class, query, query, query, limit);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Long> searchWithMappings(String query, int limit) {
        Set<String> searchTerms = new HashSet<>();
        searchTerms.add(query);
        
        // Add related terms from mappings
        for (Map.Entry<String, List<String>> entry : SEARCH_MAPPINGS.entrySet()) {
            if (query.contains(entry.getKey()) || entry.getValue().stream().anyMatch(query::contains)) {
                searchTerms.addAll(entry.getValue());
                searchTerms.add(entry.getKey());
            }
        }
        
        if (searchTerms.size() <= 1) {
            return Collections.emptyList();
        }
        
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT p.id
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                LEFT JOIN product_tags pt ON pt.product_id = p.id
                WHERE LOWER(p.status) = 'active' AND (
                """);
            
            List<Object> params = new ArrayList<>();
            boolean first = true;
            
            for (String term : searchTerms) {
                if (!first) sql.append(" OR ");
                sql.append("(");
                sql.append("LOWER(p.title) LIKE ? OR ");
                sql.append("LOWER(COALESCE(p.description, '')) LIKE ? OR ");
                sql.append("LOWER(COALESCE(b.name, '')) LIKE ? OR ");
                sql.append("LOWER(c.name) LIKE ? OR ");
                sql.append("LOWER(COALESCE(pt.tag, '')) LIKE ?");
                sql.append(")");
                
                String likePattern = "%" + term + "%";
                params.add(likePattern);
                params.add(likePattern);
                params.add(likePattern);
                params.add(likePattern);
                params.add(likePattern);
                first = false;
            }
            
            sql.append(") ORDER BY p.created_at DESC LIMIT ?");
            params.add(limit);
            
            return jdbcTemplate.queryForList(sql.toString(), Long.class, params.toArray());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<String> getTrendingSuggestions(int limit) {
        try {
            String sql = """
                SELECT DISTINCT p.title
                FROM products p
                LEFT JOIN inventory i ON i.variant_id = (
                    SELECT pv.id FROM product_variants pv WHERE pv.product_id = p.id LIMIT 1
                )
                WHERE LOWER(p.status) = 'active'
                ORDER BY COALESCE(i.stock_qty, 0) DESC, p.created_at DESC
                LIMIT ?
                """;
            
            return jdbcTemplate.queryForList(sql, String.class, limit);
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
            String sql = """
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
                """.formatted(inClause);
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