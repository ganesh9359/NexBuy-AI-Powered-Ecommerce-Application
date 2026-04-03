package com.nexbuy.modules.product.service.impl;

import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.product.dto.ProductDto;
import com.nexbuy.modules.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 48;
    private static final int MAX_HOME_LIMIT = 12;

    private final JdbcTemplate jdbcTemplate;

    public ProductServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProductDto.CatalogHomeResponse getHomeFeed(int limit) {
        int safeLimit = Math.max(4, Math.min(limit, MAX_HOME_LIMIT));
        List<ProductDto.CategorySummary> categories = getCategories();
        List<ProductDto.ProductCard> featuredProducts = loadCardsForCriteria(new CatalogCriteria(
                null, null, null, null, null, null, null, "newest", 1, safeLimit
        ));
        List<ProductDto.ProductCard> valuePicks = loadCardsForCriteria(new CatalogCriteria(
                null, null, null, null, null, null, true, "price_asc", 1, safeLimit
        ));
        ProductDto.ProductCard spotlight = featuredProducts.isEmpty() ? null : featuredProducts.get(0);

        List<ProductDto.HomeStat> stats = List.of(
                new ProductDto.HomeStat("Active products", String.valueOf(countByQuery("select count(*) from products where lower(status) = 'active'"))),
                new ProductDto.HomeStat("Categories", String.valueOf(categories.size())),
                new ProductDto.HomeStat("Brands", String.valueOf(countByQuery("""
                        select count(distinct b.id)
                        from brands b
                        join products p on p.brand_id = b.id
                        where lower(p.status) = 'active'
                        """)))
        );

        return new ProductDto.CatalogHomeResponse(
                stats,
                spotlight,
                featuredProducts,
                valuePicks,
                categories,
                loadTrendingTags(safeLimit)
        );
    }

    @Override
    public List<ProductDto.CategorySummary> getCategories() {
        return jdbcTemplate.query("""
                select c.id,
                       c.name,
                       c.slug,
                       c.description,
                       c.image_url,
                       count(p.id) as product_count
                from categories c
                left join products p on p.category_id = c.id and lower(p.status) = 'active'
                group by c.id, c.name, c.slug, c.description, c.image_url
                having count(p.id) > 0
                order by product_count desc, c.name asc
                """, (rs, rowNum) -> mapCategorySummary(rs));
    }

    @Override
    public ProductDto.ProductListResponse getCatalog(String query,
                                                     String category,
                                                     String brand,
                                                     String tag,
                                                     Integer minPrice,
                                                     Integer maxPrice,
                                                     Boolean inStock,
                                                     String sort,
                                                     int page,
                                                     int size) {
        CatalogCriteria criteria = normalizeCriteria(query, category, brand, tag, minPrice, maxPrice, inStock, sort, page, size);
        long totalItems = countProducts(criteria);
        List<ProductDto.ProductCard> items = totalItems == 0
                ? Collections.emptyList()
                : loadCardsByIds(findProductIds(criteria));

        ProductDto.CatalogFilters filters = new ProductDto.CatalogFilters(
                getCategories(),
                loadBrandFacets(new CatalogCriteria(criteria.query(), criteria.category(), null, criteria.tag(),
                        criteria.minPrice(), criteria.maxPrice(), criteria.inStock(), criteria.sort(), criteria.page(), criteria.size())),
                loadTagFacets(new CatalogCriteria(criteria.query(), criteria.category(), criteria.brand(), null,
                        criteria.minPrice(), criteria.maxPrice(), criteria.inStock(), criteria.sort(), criteria.page(), criteria.size())),
                loadPriceRange(new CatalogCriteria(criteria.query(), criteria.category(), criteria.brand(), criteria.tag(),
                        null, null, criteria.inStock(), criteria.sort(), criteria.page(), criteria.size()))
        );

        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / criteria.size());
        return new ProductDto.ProductListResponse(
                items,
                criteria.page(),
                criteria.size(),
                totalItems,
                totalPages,
                criteria.query(),
                criteria.category(),
                criteria.brand(),
                criteria.tag(),
                criteria.sort(),
                filters
        );
    }

    @Override
    public ProductDto.ProductListResponse search(String query,
                                                 String category,
                                                 String brand,
                                                 String tag,
                                                 Integer minPrice,
                                                 Integer maxPrice,
                                                 Boolean inStock,
                                                 String sort,
                                                 int page,
                                                 int size) {
        return getCatalog(query, category, brand, tag, minPrice, maxPrice, inStock, sort, page, size);
    }

    @Override
    public ProductDto.ProductDetailResponse getProduct(String slug, int relatedLimit) {
        ProductSnapshot product = loadProductBySlug(slug)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));

        return new ProductDto.ProductDetailResponse(product.toCard(), loadRelatedProducts(product, relatedLimit));
    }

    @Override
    public List<ProductDto.ProductCard> getRelatedProducts(String slug, int limit) {
        ProductSnapshot product = loadProductBySlug(slug)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
        return loadRelatedProducts(product, limit);
    }

    private CatalogCriteria normalizeCriteria(String query,
                                              String category,
                                              String brand,
                                              String tag,
                                              Integer minPrice,
                                              Integer maxPrice,
                                              Boolean inStock,
                                              String sort,
                                              int page,
                                              int size) {
        String normalizedQuery = emptyToNull(query);
        String normalizedCategory = normalizeSlug(category);
        String normalizedBrand = normalizeSlug(brand);
        String normalizedTag = normalizeTag(tag);
        Integer normalizedMinPrice = minPrice != null && minPrice >= 0 ? minPrice : null;
        Integer normalizedMaxPrice = maxPrice != null && maxPrice >= 0 ? maxPrice : null;

        if (normalizedMinPrice != null && normalizedMaxPrice != null && normalizedMinPrice > normalizedMaxPrice) {
            int swap = normalizedMinPrice;
            normalizedMinPrice = normalizedMaxPrice;
            normalizedMaxPrice = swap;
        }

        int normalizedPage = Math.max(page, 1);
        int normalizedSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String normalizedSort = normalizeSort(sort, normalizedQuery);

        return new CatalogCriteria(
                normalizedQuery,
                normalizedCategory,
                normalizedBrand,
                normalizedTag,
                normalizedMinPrice,
                normalizedMaxPrice,
                inStock,
                normalizedSort,
                normalizedPage,
                normalizedSize
        );
    }

    private String normalizeSort(String sort, String query) {
        String requested = emptyToNull(sort);
        if (requested == null) {
            return query == null ? "newest" : "relevance";
        }

        return switch (requested.trim().toLowerCase(Locale.ROOT)) {
            case "relevance", "price_asc", "price_desc", "title", "stock_desc", "newest" -> requested.trim().toLowerCase(Locale.ROOT);
            default -> query == null ? "newest" : "relevance";
        };
    }

    private long countProducts(CatalogCriteria criteria) {
        StringBuilder sql = new StringBuilder("select count(distinct p.id) ").append(baseFromClause());
        List<Object> args = new ArrayList<>();
        appendFilters(criteria, sql, args);
        Number count = jdbcTemplate.queryForObject(sql.toString(), Number.class, args.toArray());
        return count == null ? 0L : count.longValue();
    }

    private List<Long> findProductIds(CatalogCriteria criteria) {
        StringBuilder sql = new StringBuilder("select p.id ").append(baseFromClause());
        List<Object> args = new ArrayList<>();
        appendFilters(criteria, sql, args);
        appendSort(criteria, sql, args);
        sql.append(" limit ? offset ?");
        args.add(criteria.size());
        args.add((criteria.page() - 1) * criteria.size());

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("id"), args.toArray());
    }

    private List<ProductDto.ProductCard> loadCardsForCriteria(CatalogCriteria criteria) {
        return loadCardsByIds(findProductIds(criteria));
    }

    private List<ProductDto.ProductCard> loadCardsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Integer> orderById = new LinkedHashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            orderById.put(ids.get(index), index);
        }

        String sql = selectClause() + baseFromClause() + " where p.id in (" + placeholders(ids.size()) + ")";
        Map<Long, ProductSnapshot> snapshotsById = new LinkedHashMap<>();
        jdbcTemplate.query(sql, ids.toArray(), rs -> {
            ProductSnapshot snapshot = mapSnapshot(rs);
            snapshotsById.put(snapshot.id, snapshot);
        });

        List<ProductSnapshot> hydratedSnapshots = new ArrayList<>(snapshotsById.values());
        hydrateRelationships(hydratedSnapshots);

        return hydratedSnapshots.stream()
                .sorted((left, right) -> Integer.compare(
                        orderById.getOrDefault(left.id, Integer.MAX_VALUE),
                        orderById.getOrDefault(right.id, Integer.MAX_VALUE)
                ))
                .map(ProductSnapshot::toCard)
                .collect(Collectors.toList());
    }

    private Optional<ProductSnapshot> loadProductBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);
        if (normalizedSlug == null) {
            return Optional.empty();
        }

        String sql = selectClause() + baseFromClause() + " where lower(p.status) = 'active' and p.slug = ? limit 1";
        List<ProductSnapshot> matches = jdbcTemplate.query(sql, (rs, rowNum) -> mapSnapshot(rs), normalizedSlug);
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        ProductSnapshot snapshot = matches.get(0);
        hydrateRelationships(List.of(snapshot));
        return Optional.of(snapshot);
    }

    private List<ProductDto.ProductCard> loadRelatedProducts(ProductSnapshot product, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 8));
        List<String> productTags = product.tags == null ? Collections.emptyList() : product.tags;

        StringBuilder sql = new StringBuilder("select p.id ")
                .append(baseFromClause())
                .append(" where lower(p.status) = 'active' and p.id <> ? and c.slug = ?");
        List<Object> args = new ArrayList<>();
        args.add(product.id);
        args.add(product.categorySlug);

        if (!productTags.isEmpty()) {
            sql.append(" order by (select count(*) from product_tags ptr where ptr.product_id = p.id and ptr.tag in (")
                    .append(placeholders(productTags.size()))
                    .append(")) desc, abs(coalesce(pv.price_cents, 0) - ?) asc, p.created_at desc");
            args.addAll(productTags);
            args.add(product.priceCents);
        } else {
            sql.append(" order by abs(coalesce(pv.price_cents, 0) - ?) asc, p.created_at desc");
            args.add(product.priceCents);
        }

        sql.append(" limit ?");
        args.add(safeLimit);

        List<Long> relatedIds = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("id"), args.toArray());
        return loadCardsByIds(relatedIds);
    }

    private void hydrateRelationships(List<ProductSnapshot> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        List<Long> productIds = products.stream()
                .map(product -> product.id)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, List<String>> tagsByProductId = loadTagsByProductIds(productIds);
        Map<Long, List<ProductDto.Media>> mediaByProductId = loadMediaByProductIds(productIds);

        for (ProductSnapshot product : products) {
            product.tags = new ArrayList<>(tagsByProductId.getOrDefault(product.id, Collections.emptyList()));
            product.media = new ArrayList<>(mediaByProductId.getOrDefault(product.id, Collections.emptyList()));
            if ((product.coverImage == null || product.coverImage.isBlank()) && !product.media.isEmpty()) {
                product.coverImage = product.media.get(0).url();
            }
        }
    }

    private Map<Long, List<String>> loadTagsByProductIds(List<Long> productIds) {
        Map<Long, List<String>> tagsByProductId = new LinkedHashMap<>();
        if (productIds.isEmpty()) {
            return tagsByProductId;
        }

        String sql = "select product_id, tag from product_tags where product_id in (" + placeholders(productIds.size()) + ") order by product_id asc, tag asc";
        jdbcTemplate.query(sql, productIds.toArray(), (RowCallbackHandler) rs -> tagsByProductId
                .computeIfAbsent(rs.getLong("product_id"), ignored -> new ArrayList<>())
                .add(rs.getString("tag")));
        return tagsByProductId;
    }

    private Map<Long, List<ProductDto.Media>> loadMediaByProductIds(List<Long> productIds) {
        Map<Long, List<ProductDto.Media>> mediaByProductId = new LinkedHashMap<>();
        if (productIds.isEmpty()) {
            return mediaByProductId;
        }

        String sql = """
                select id, product_id, url, alt_text, sort_order
                from product_media
                where product_id in (%s)
                order by product_id asc, sort_order asc, id asc
                """.formatted(placeholders(productIds.size()));

        jdbcTemplate.query(sql, productIds.toArray(), (RowCallbackHandler) rs -> mediaByProductId
                .computeIfAbsent(rs.getLong("product_id"), ignored -> new ArrayList<>())
                .add(new ProductDto.Media(
                        rs.getLong("id"),
                        rs.getString("url"),
                        rs.getString("alt_text"),
                        rs.getInt("sort_order")
                )));
        return mediaByProductId;
    }

    private List<ProductDto.BrandSummary> loadBrandFacets(CatalogCriteria criteria) {
        StringBuilder sql = new StringBuilder("""
                select b.id, b.name, b.slug, count(distinct p.id) as product_count
                """).append(baseFromClause());
        List<Object> args = new ArrayList<>();
        appendFilters(criteria, sql, args);
        sql.append("""
                 and b.id is not null
                 group by b.id, b.name, b.slug
                 having count(distinct p.id) > 0
                 order by product_count desc, b.name asc
                 limit 12
                """);

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> new ProductDto.BrandSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getLong("product_count")
        ));
    }

    private List<ProductDto.TagSummary> loadTagFacets(CatalogCriteria criteria) {
        StringBuilder sql = new StringBuilder("""
                select pt.tag, count(distinct p.id) as product_count
                from product_tags pt
                join products p on p.id = pt.product_id
                join categories c on c.id = p.category_id
                left join brands b on b.id = p.brand_id
                left join product_variants pv on pv.id = (
                    select pv2.id from product_variants pv2 where pv2.product_id = p.id order by pv2.id asc limit 1
                )
                left join inventory i on i.variant_id = pv.id
                """);
        List<Object> args = new ArrayList<>();
        appendProductPredicates(criteria, sql, args);
        sql.append("""
                 group by pt.tag
                 order by product_count desc, pt.tag asc
                 limit 16
                """);

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> new ProductDto.TagSummary(
                rs.getString("tag"),
                rs.getLong("product_count")
        ));
    }

    private ProductDto.PriceRange loadPriceRange(CatalogCriteria criteria) {
        StringBuilder sql = new StringBuilder("select min(pv.price_cents) as min_cents, max(pv.price_cents) as max_cents ")
                .append(baseFromClause());
        List<Object> args = new ArrayList<>();
        appendFilters(criteria, sql, args);

        return jdbcTemplate.query(sql.toString(), args.toArray(), rs -> {
            if (!rs.next()) {
                return new ProductDto.PriceRange(null, null);
            }
            return new ProductDto.PriceRange(
                    rs.getObject("min_cents", Integer.class),
                    rs.getObject("max_cents", Integer.class)
            );
        });
    }

    private List<ProductDto.TagSummary> loadTrendingTags(int limit) {
        int safeLimit = Math.max(4, Math.min(limit, 12));
        return jdbcTemplate.query("""
                select pt.tag, count(*) as product_count
                from product_tags pt
                join products p on p.id = pt.product_id
                where lower(p.status) = 'active'
                group by pt.tag
                order by product_count desc, pt.tag asc
                limit ?
                """, (rs, rowNum) -> new ProductDto.TagSummary(
                rs.getString("tag"),
                rs.getLong("product_count")
        ), safeLimit);
    }

    private void appendFilters(CatalogCriteria criteria, StringBuilder sql, List<Object> args) {
        appendProductPredicates(criteria, sql, args);
    }

    private void appendProductPredicates(CatalogCriteria criteria, StringBuilder sql, List<Object> args) {
        sql.append(" where 1 = 1");
        sql.append(" and lower(p.status) = 'active'");

        if (criteria.category() != null) {
            sql.append(" and c.slug = ?");
            args.add(criteria.category());
        }
        if (criteria.brand() != null) {
            sql.append(" and b.slug = ?");
            args.add(criteria.brand());
        }
        if (criteria.tag() != null) {
            sql.append(" and exists (select 1 from product_tags ptf where ptf.product_id = p.id and lower(ptf.tag) = ?)");
            args.add(criteria.tag());
        }
        if (criteria.minPrice() != null) {
            sql.append(" and coalesce(pv.price_cents, 0) >= ?");
            args.add(criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            sql.append(" and coalesce(pv.price_cents, 0) <= ?");
            args.add(criteria.maxPrice());
        }
        if (Boolean.TRUE.equals(criteria.inStock())) {
            sql.append(" and coalesce(i.stock_qty, 0) > 0");
        }
        if (criteria.query() != null) {
            String queryLike = "%" + criteria.query().toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     and (
                        lower(p.title) like ?
                        or lower(coalesce(p.description, '')) like ?
                        or lower(c.name) like ?
                        or lower(coalesce(b.name, '')) like ?
                        or exists (
                            select 1
                            from product_tags pts
                            where pts.product_id = p.id and lower(pts.tag) like ?
                        )
                     )
                    """);
            args.add(queryLike);
            args.add(queryLike);
            args.add(queryLike);
            args.add(queryLike);
            args.add(queryLike);
        }
    }

    private void appendSort(CatalogCriteria criteria, StringBuilder sql, List<Object> args) {
        switch (criteria.sort()) {
            case "price_asc" -> sql.append(" order by coalesce(pv.price_cents, 0) asc, p.created_at desc");
            case "price_desc" -> sql.append(" order by coalesce(pv.price_cents, 0) desc, p.created_at desc");
            case "title" -> sql.append(" order by p.title asc, p.created_at desc");
            case "stock_desc" -> sql.append(" order by coalesce(i.stock_qty, 0) desc, p.created_at desc");
            case "relevance" -> {
                if (criteria.query() == null) {
                    sql.append(" order by p.created_at desc");
                    return;
                }
                String exact = criteria.query().toLowerCase(Locale.ROOT);
                String prefix = exact + "%";
                String contains = "%" + exact + "%";
                sql.append("""
                        order by case
                            when lower(p.title) = ? then 0
                            when lower(p.title) like ? then 1
                            when lower(coalesce(b.name, '')) = ? then 2
                            when lower(coalesce(b.name, '')) like ? then 3
                            when exists (
                                select 1 from product_tags ptr where ptr.product_id = p.id and lower(ptr.tag) = ?
                            ) then 4
                            when exists (
                                select 1 from product_tags ptr2 where ptr2.product_id = p.id and lower(ptr2.tag) like ?
                            ) then 5
                            else 6
                        end, p.created_at desc
                        """);
                args.add(exact);
                args.add(prefix);
                args.add(exact);
                args.add(contains);
                args.add(exact);
                args.add(contains);
            }
            default -> sql.append(" order by p.created_at desc");
        }
    }

    private ProductSnapshot mapSnapshot(ResultSet rs) throws SQLException {
        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.id = rs.getLong("id");
        snapshot.slug = rs.getString("slug");
        snapshot.title = rs.getString("title");
        snapshot.description = rs.getString("description");
        snapshot.coverImage = rs.getString("cover_image");
        snapshot.categoryName = rs.getString("category_name");
        snapshot.categorySlug = rs.getString("category_slug");
        snapshot.brandName = rs.getString("brand_name");
        snapshot.brandSlug = rs.getString("brand_slug");
        snapshot.sku = rs.getString("sku");
        snapshot.variantName = rs.getString("variant_name");
        snapshot.priceCents = rs.getInt("price_cents");
        snapshot.compareAtCents = rs.getObject("compare_at_cents", Integer.class);
        snapshot.currency = rs.getString("currency");
        snapshot.stockQty = rs.getInt("stock_qty");
        snapshot.lowStockThreshold = rs.getInt("low_stock_threshold");
        snapshot.backorderAllowed = rs.getBoolean("is_backorder_allowed");
        snapshot.tags = new ArrayList<>();
        snapshot.media = new ArrayList<>();
        return snapshot;
    }

    private ProductDto.CategorySummary mapCategorySummary(ResultSet rs) throws SQLException {
        return new ProductDto.CategorySummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getString("image_url"),
                rs.getLong("product_count")
        );
    }

    private long countByQuery(String sql) {
        Number count = jdbcTemplate.queryForObject(sql, Number.class);
        return count == null ? 0L : count.longValue();
    }

    private String selectClause() {
        return """
                select p.id,
                       p.slug,
                       p.title,
                       p.description,
                       p.cover_image,
                       c.name as category_name,
                       c.slug as category_slug,
                       b.name as brand_name,
                       b.slug as brand_slug,
                       pv.sku,
                       pv.name as variant_name,
                       coalesce(pv.price_cents, 0) as price_cents,
                       pv.compare_at_cents,
                       pv.currency,
                       coalesce(i.stock_qty, 0) as stock_qty,
                       coalesce(i.low_stock_threshold, 5) as low_stock_threshold,
                       coalesce(i.is_backorder_allowed, false) as is_backorder_allowed
                """;
    }

    private String baseFromClause() {
        return """
                from products p
                join categories c on c.id = p.category_id
                left join brands b on b.id = p.brand_id
                left join product_variants pv on pv.id = (
                    select pv2.id from product_variants pv2 where pv2.product_id = p.id order by pv2.id asc limit 1
                )
                left join inventory i on i.variant_id = pv.id
                """;
    }

    private String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSlug(String value) {
        String normalized = emptyToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String normalizeTag(String value) {
        String normalized = emptyToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private record CatalogCriteria(String query,
                                   String category,
                                   String brand,
                                   String tag,
                                   Integer minPrice,
                                   Integer maxPrice,
                                   Boolean inStock,
                                   String sort,
                                   int page,
                                   int size) {
    }

    private static final class ProductSnapshot {
        private Long id;
        private String slug;
        private String title;
        private String description;
        private String coverImage;
        private String categoryName;
        private String categorySlug;
        private String brandName;
        private String brandSlug;
        private String sku;
        private String variantName;
        private int priceCents;
        private Integer compareAtCents;
        private String currency;
        private int stockQty;
        private int lowStockThreshold;
        private boolean backorderAllowed;
        private List<String> tags = new ArrayList<>();
        private List<ProductDto.Media> media = new ArrayList<>();

        private ProductDto.ProductCard toCard() {
            return new ProductDto.ProductCard(
                    id,
                    slug,
                    title,
                    description,
                    coverImage,
                    categoryName,
                    categorySlug,
                    brandName,
                    brandSlug,
                    sku,
                    variantName,
                    priceCents,
                    compareAtCents,
                    currency == null ? "INR" : currency,
                    toStockSummary(),
                    List.copyOf(new LinkedHashSet<>(tags)),
                    List.copyOf(media)
            );
        }

        private ProductDto.StockSummary toStockSummary() {
            boolean inStock = stockQty > 0;
            boolean lowStock = inStock && stockQty <= Math.max(lowStockThreshold, 1);
            String label;
            if (inStock && lowStock) {
                label = "Only " + stockQty + " left";
            } else if (inStock) {
                label = "In stock";
            } else {
                label = "Out of stock";
            }

            return new ProductDto.StockSummary(
                    stockQty,
                    lowStockThreshold,
                    backorderAllowed,
                    inStock,
                    lowStock,
                    label
            );
        }
    }
}



