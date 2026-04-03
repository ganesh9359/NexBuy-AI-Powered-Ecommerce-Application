package com.nexbuy.modules.product.dto;

import java.util.List;

public final class ProductDto {

    private ProductDto() {
    }

    public record HomeStat(String label, String value) {
    }

    public record CategorySummary(Long id,
                                  String name,
                                  String slug,
                                  String description,
                                  String imageUrl,
                                  long productCount) {
    }

    public record BrandSummary(Long id,
                               String name,
                               String slug,
                               long productCount) {
    }

    public record TagSummary(String tag, long productCount) {
    }

    public record PriceRange(Integer minCents, Integer maxCents) {
    }

    public record CatalogFilters(List<CategorySummary> categories,
                                 List<BrandSummary> brands,
                                 List<TagSummary> tags,
                                 PriceRange priceRange) {
    }

    public record Media(Long id, String url, String altText, int sortOrder) {
    }

    public record StockSummary(int stockQty,
                               int lowStockThreshold,
                               boolean backorderAllowed,
                               boolean inStock,
                               boolean lowStock,
                               String label) {
    }

    public record ProductCard(Long id,
                              String slug,
                              String title,
                              String description,
                              String coverImage,
                              String categoryName,
                              String categorySlug,
                              String brandName,
                              String brandSlug,
                              String sku,
                              String variantName,
                              int priceCents,
                              Integer compareAtCents,
                              String currency,
                              StockSummary stock,
                              List<String> tags,
                              List<Media> media) {
    }

    public record CatalogHomeResponse(List<HomeStat> stats,
                                      ProductCard spotlightProduct,
                                      List<ProductCard> featuredProducts,
                                      List<ProductCard> valuePicks,
                                      List<CategorySummary> categories,
                                      List<TagSummary> trendingTags) {
    }

    public record ProductListResponse(List<ProductCard> items,
                                      int page,
                                      int size,
                                      long totalItems,
                                      int totalPages,
                                      String query,
                                      String category,
                                      String brand,
                                      String tag,
                                      String sort,
                                      CatalogFilters filters) {
    }

    public record ProductDetailResponse(ProductCard product,
                                        List<ProductCard> relatedProducts) {
    }
}
