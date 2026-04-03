package com.nexbuy.modules.product.service;

import com.nexbuy.modules.product.dto.ProductDto;

import java.util.List;

public interface ProductService {

    ProductDto.CatalogHomeResponse getHomeFeed(int limit);

    List<ProductDto.CategorySummary> getCategories();

    ProductDto.ProductListResponse getCatalog(String query,
                                              String category,
                                              String brand,
                                              String tag,
                                              Integer minPrice,
                                              Integer maxPrice,
                                              Boolean inStock,
                                              String sort,
                                              int page,
                                              int size);

    ProductDto.ProductListResponse search(String query,
                                          String category,
                                          String brand,
                                          String tag,
                                          Integer minPrice,
                                          Integer maxPrice,
                                          Boolean inStock,
                                          String sort,
                                          int page,
                                          int size);

    ProductDto.ProductDetailResponse getProduct(String slug, int relatedLimit);

    List<ProductDto.ProductCard> getRelatedProducts(String slug, int limit);
}
