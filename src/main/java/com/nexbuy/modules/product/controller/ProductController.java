package com.nexbuy.modules.product.controller;

import com.nexbuy.modules.product.dto.ProductDto;
import com.nexbuy.modules.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/home")
    public ResponseEntity<ProductDto.CatalogHomeResponse> getHomeFeed(
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(productService.getHomeFeed(limit));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ProductDto.CategorySummary>> getCategories() {
        return ResponseEntity.ok(productService.getCategories());
    }

    @GetMapping("/catalog")
    public ResponseEntity<ProductDto.ProductListResponse> getCatalog(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.getCatalog(q, category, brand, tag, minPrice, maxPrice, inStock, sort, page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<ProductDto.ProductListResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.search(q, category, brand, tag, minPrice, maxPrice, inStock, sort, page, size));
    }

    @GetMapping("/{slug}/related")
    public ResponseEntity<List<ProductDto.ProductCard>> getRelatedProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "4") int limit
    ) {
        return ResponseEntity.ok(productService.getRelatedProducts(slug, limit));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDto.ProductDetailResponse> getProduct(
            @PathVariable String slug,
            @RequestParam(defaultValue = "4") int relatedLimit
    ) {
        return ResponseEntity.ok(productService.getProduct(slug, relatedLimit));
    }
}
