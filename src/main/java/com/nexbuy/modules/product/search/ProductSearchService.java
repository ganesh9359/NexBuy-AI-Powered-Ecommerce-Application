package com.nexbuy.modules.product.search;

import com.nexbuy.modules.product.dto.ProductDto;
import java.util.List;

public interface ProductSearchService {
    
    /**
     * Enhanced search with intelligent matching and recommendations
     */
    ProductDto.ProductListResponse enhancedSearch(
        String query,
        String category,
        String brand,
        String tag,
        Integer minPrice,
        Integer maxPrice,
        Boolean inStock,
        String sort,
        int page,
        int size
    );
    
    /**
     * Get intelligent search suggestions based on partial query
     */
    List<String> getSearchSuggestions(String partialQuery, int limit);
    
    /**
     * Get recommended products when no search results found
     */
    List<ProductDto.ProductCard> getRecommendedProducts(String originalQuery, int limit);
    
    /**
     * Fuzzy search with typo tolerance
     */
    List<ProductDto.ProductCard> fuzzySearch(String query, int limit);
    
    /**
     * Search by similar products (visual/feature similarity)
     */
    List<ProductDto.ProductCard> findSimilarProducts(Long productId, int limit);
}
