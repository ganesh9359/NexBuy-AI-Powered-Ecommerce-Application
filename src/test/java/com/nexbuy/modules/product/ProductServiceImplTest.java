package com.nexbuy.modules.product;

import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.product.dto.ProductDto;
import com.nexbuy.modules.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceImplTest {

    @Mock JdbcTemplate jdbcTemplate;

    ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(jdbcTemplate);
    }

    // ── getCategories ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCategories")
    class GetCategoriesTests {

        @Test
        @DisplayName("Returns list of categories from DB")
        void getCategories_returnsList() {
            ProductDto.CategorySummary cat = new ProductDto.CategorySummary(1L, "Electronics", "electronics", null, null, 5L);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(cat));

            List<ProductDto.CategorySummary> result = productService.getCategories();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("Returns empty list when no categories exist")
        void getCategories_empty() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

            assertThat(productService.getCategories()).isEmpty();
        }
    }

    // ── getCatalog ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCatalog")
    class GetCatalogTests {

        @Test
        @DisplayName("Returns empty response when no products found")
        void getCatalog_noProducts() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), any(Object[].class))).thenReturn(0L);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

            ProductDto.ProductListResponse res = productService.getCatalog(
                    null, null, null, null, null, null, null, null, 1, 12);

            assertThat(res.items()).isEmpty();
            assertThat(res.totalItems()).isZero();
            assertThat(res.totalPages()).isZero();
        }

        @Test
        @DisplayName("Page is normalized to minimum 1")
        void getCatalog_pageNormalized() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), any(Object[].class))).thenReturn(0L);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

            ProductDto.ProductListResponse res = productService.getCatalog(
                    null, null, null, null, null, null, null, null, -5, 12);

            assertThat(res.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("Size is capped at MAX_PAGE_SIZE (48)")
        void getCatalog_sizeCapped() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), any(Object[].class))).thenReturn(0L);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

            ProductDto.ProductListResponse res = productService.getCatalog(
                    null, null, null, null, null, null, null, null, 1, 999);

            assertThat(res.size()).isEqualTo(48);
        }

        @Test
        @DisplayName("Min/max price swap when min > max")
        void getCatalog_priceSwap() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), any(Object[].class))).thenReturn(0L);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

            // Should not throw — prices are swapped internally
            assertThatNoException().isThrownBy(() ->
                    productService.getCatalog(null, null, null, null, 50000, 10000, null, null, 1, 12));
        }

        @Test
        @DisplayName("Invalid sort falls back to newest")
        void getCatalog_invalidSortFallback() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), any(Object[].class))).thenReturn(0L);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

            ProductDto.ProductListResponse res = productService.getCatalog(
                    null, null, null, null, null, null, null, "invalid_sort", 1, 12);

            assertThat(res.sort()).isEqualTo("newest");
        }
    }

    // ── getProduct ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProduct")
    class GetProductTests {

        @Test
        @DisplayName("Non-existent slug throws NOT_FOUND")
        void getProduct_notFound() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> productService.getProduct("non-existent-slug", 4))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Null slug throws NOT_FOUND")
        void getProduct_nullSlug() {
            assertThatThrownBy(() -> productService.getProduct(null, 4))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Blank slug throws NOT_FOUND")
        void getProduct_blankSlug() {
            assertThatThrownBy(() -> productService.getProduct("   ", 4))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("Search delegates to getCatalog and returns same structure")
        void search_delegatesToCatalog() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), any(Object[].class))).thenReturn(0L);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

            ProductDto.ProductListResponse res = productService.search(
                    "laptop", null, null, null, null, null, null, "relevance", 1, 12);

            assertThat(res).isNotNull();
            assertThat(res.query()).isEqualTo("laptop");
        }
    }
}
