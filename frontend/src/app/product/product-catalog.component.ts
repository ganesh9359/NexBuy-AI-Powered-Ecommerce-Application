import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Data, ParamMap, Router } from '@angular/router';
import { combineLatest } from 'rxjs';
import { CatalogListingResponse, CatalogProductCard, ProductApiService } from './product-api.service';

type PageMode = 'catalog' | 'category' | 'search';

interface ViewState {
  mode: PageMode;
  q?: string;
  category?: string;
  brand?: string;
  tag?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  sort?: string;
  page: number;
  size: number;
}

@Component({
  selector: 'app-product-catalog',
  templateUrl: './product-catalog.component.html',
  styleUrls: ['./product-catalog.component.scss']
})
export class ProductCatalogComponent implements OnInit {
  response?: CatalogListingResponse;
  loading = true;
  error = '';
  currentTitle = 'Browse catalog';
  currentSubtitle = 'Explore the live product feed with filters for brand, tag, price, and stock.';
  activeCategorySlug = '';
  activeCategoryLabel = '';
  activeQuery = '';
  mode: PageMode = 'catalog';

  readonly sortOptions = [
    { value: 'relevance', label: 'Relevance' },
    { value: 'newest', label: 'Newest first' },
    { value: 'price_asc', label: 'Price: low to high' },
    { value: 'price_desc', label: 'Price: high to low' },
    { value: 'stock_desc', label: 'Stock first' },
    { value: 'title', label: 'Title A-Z' }
  ];

  readonly pageSizes = [12, 24, 36];

  filterForm = {
    brand: '',
    tag: '',
    minPrice: '',
    maxPrice: '',
    inStock: false,
    sort: 'newest'
  };

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });
  private readonly supportedSorts = new Set(this.sortOptions.map((option) => option.value));

  private currentState?: ViewState;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly productApi: ProductApiService
  ) {}

  ngOnInit(): void {
    combineLatest([this.route.data, this.route.paramMap, this.route.queryParamMap]).subscribe({
      next: ([data, params, queryParams]) => {
        const state = this.buildState(data, params, queryParams);
        this.currentState = state;
        this.mode = state.mode;
        this.activeCategorySlug = state.category || '';
        this.activeQuery = state.q || '';
        this.syncForm(state);
        this.load(state);
      }
    });
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  primaryImage(product: CatalogProductCard): string {
    return product.coverImage || product.media?.[0]?.url || 'assets/logo.svg';
  }

  visiblePages(): number[] {
    if (!this.response || this.response.totalPages <= 1) {
      return [];
    }

    const pages: number[] = [];
    const start = Math.max(1, this.response.page - 2);
    const end = Math.min(this.response.totalPages, this.response.page + 2);
    for (let page = start; page <= end; page += 1) {
      pages.push(page);
    }
    return pages;
  }

  applyFilters(): void {
    this.sanitizeFilterForm();
    const path = this.pathForMode();
    this.router.navigate(path, { queryParams: this.buildQueryParams() });
  }

  clearFilters(): void {
    const defaultSort = this.defaultSort();
    this.filterForm = {
      brand: '',
      tag: '',
      minPrice: '',
      maxPrice: '',
      inStock: false,
      sort: defaultSort
    };

    const queryParams: Record<string, string | number | boolean | null> = {};
    if (this.mode === 'search' && this.activeQuery) {
      queryParams['q'] = this.activeQuery;
    }
    this.router.navigate(this.pathForMode(), { queryParams });
  }

  selectTag(tag: string): void {
    this.filterForm.tag = this.filterForm.tag === tag ? '' : tag;
    this.applyFilters();
  }

  searchSimilar(query: string): void {
    this.router.navigate(['/product/search'], { queryParams: { q: query } });
  }

  goToCategory(slug: string): void {
    this.router.navigate(['/product/category', slug]);
  }

  setPage(page: number): void {
    if (!this.response || page < 1 || page > this.response.totalPages || page === this.response.page) {
      return;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge'
    });
  }

  changePageSize(size: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { size, page: 1 },
      queryParamsHandling: 'merge'
    });
  }

  retry(): void {
    if (this.currentState) {
      this.load(this.currentState);
    }
  }

  private load(state: ViewState): void {
    this.loading = true;
    this.error = '';

    const query = {
      q: state.q,
      category: state.category,
      brand: state.brand,
      tag: state.tag,
      minPrice: state.minPrice,
      maxPrice: state.maxPrice,
      inStock: state.inStock,
      sort: state.sort,
      page: state.page,
      size: state.size
    };

    const source = state.mode === 'search' && state.q
      ? this.productApi.search(query)
      : this.productApi.getCatalog(query);

    source.subscribe({
      next: (response) => {
        this.response = response;
        this.loading = false;
        this.applyContext(state, response);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load catalog results.';
        this.loading = false;
      }
    });
  }

  private applyContext(state: ViewState, response: CatalogListingResponse): void {
    this.activeCategoryLabel = response.filters.categories.find((category) => category.slug === state.category)?.name
      || this.prettyLabel(state.category);
    this.filterForm = {
      ...this.filterForm,
      sort: this.normalizeSort(response.sort, state.q)
    };

    if (state.mode === 'category') {
      this.currentTitle = this.activeCategoryLabel || 'Category browse';
      this.currentSubtitle = `Showing ${response.totalItems} products with live stock, tags, and pricing in this category.`;
      return;
    }

    if (state.mode === 'search') {
      this.currentTitle = state.q ? `Results for "${state.q}"` : 'Search the catalog';
      this.currentSubtitle = state.q
        ? 'Search results are coming from the real catalog API with matching brands, tags, and stock states.'
        : 'Type a search in the header to query the live catalog.';
      return;
    }

    this.currentTitle = 'Browse the full catalog';
    this.currentSubtitle = 'Filter down the seeded product inventory by brand, tag, price range, and live stock status.';
  }

  private buildState(data: Data, params: ParamMap, queryParams: ParamMap): ViewState {
    const mode = (data['mode'] as PageMode) || 'catalog';
    return {
      mode,
      q: this.trimOrUndefined(queryParams.get('q')),
      category: mode === 'category' ? this.trimOrUndefined(params.get('slug')) : this.trimOrUndefined(queryParams.get('category')),
      brand: this.trimOrUndefined(queryParams.get('brand')),
      tag: this.trimOrUndefined(queryParams.get('tag')),
      minPrice: this.parseNumber(queryParams.get('minPrice')),
      maxPrice: this.parseNumber(queryParams.get('maxPrice')),
      inStock: queryParams.get('inStock') === 'true' ? true : undefined,
      sort: this.trimOrUndefined(queryParams.get('sort')),
      page: this.parseNumber(queryParams.get('page')) || 1,
      size: this.parseNumber(queryParams.get('size')) || 12
    };
  }

  private syncForm(state: ViewState): void {
    const minPrice = this.normalizePriceCents(state.minPrice);
    const maxPrice = this.normalizePriceCents(state.maxPrice);
    const [safeMin, safeMax] = this.sortedPrices(minPrice, maxPrice);

    this.filterForm = {
      brand: state.brand || '',
      tag: state.tag || '',
      minPrice: safeMin ? String(Math.round(safeMin / 100)) : '',
      maxPrice: safeMax ? String(Math.round(safeMax / 100)) : '',
      inStock: !!state.inStock,
      sort: this.normalizeSort(state.sort, state.q)
    };
  }

  private buildQueryParams(): Record<string, string | number | boolean | null> {
    this.sanitizeFilterForm();
    const sort = this.normalizeSort(this.filterForm.sort, this.activeQuery || undefined);
    return {
      q: this.mode === 'search' ? (this.activeQuery || null) : null,
      brand: this.filterForm.brand || null,
      tag: this.filterForm.tag || null,
      minPrice: this.toCents(this.filterForm.minPrice),
      maxPrice: this.toCents(this.filterForm.maxPrice),
      inStock: this.filterForm.inStock ? true : null,
      sort: sort === this.defaultSort() ? null : sort,
      page: null,
      size: this.response?.size && this.response.size !== 12 ? this.response.size : null
    };
  }

  private pathForMode(): string[] {
    if (this.mode === 'category' && this.activeCategorySlug) {
      return ['/product/category', this.activeCategorySlug];
    }
    if (this.mode === 'search') {
      return ['/product/search'];
    }
    return ['/product/catalog'];
  }

  private sanitizeFilterForm(): void {
    const minPrice = this.toCents(this.filterForm.minPrice);
    const maxPrice = this.toCents(this.filterForm.maxPrice);
    const [safeMin, safeMax] = this.sortedPrices(minPrice, maxPrice);

    this.filterForm = {
      ...this.filterForm,
      minPrice: safeMin ? String(Math.round(safeMin / 100)) : '',
      maxPrice: safeMax ? String(Math.round(safeMax / 100)) : '',
      sort: this.normalizeSort(this.filterForm.sort, this.activeQuery || undefined)
    };
  }

  private sortedPrices(minPrice: number | null, maxPrice: number | null): [number | null, number | null] {
    if (minPrice !== null && maxPrice !== null && minPrice > maxPrice) {
      return [maxPrice, minPrice];
    }
    return [minPrice, maxPrice];
  }

  private defaultSort(): string {
    return this.activeQuery ? 'relevance' : 'newest';
  }

  private normalizeSort(sort?: string, query?: string): string {
    const fallback = query ? 'relevance' : 'newest';
    const normalized = (sort || '').trim().toLowerCase();
    if (!normalized || !this.supportedSorts.has(normalized)) {
      return fallback;
    }
    if (!query && normalized === 'relevance') {
      return 'newest';
    }
    return normalized;
  }

  private toCents(value: string): number | null {
    const numericValue = Number(value);
    if (!Number.isFinite(numericValue) || numericValue <= 0) {
      return null;
    }
    return Math.round(numericValue * 100);
  }

  private normalizePriceCents(value?: number): number | null {
    return typeof value === 'number' && Number.isFinite(value) && value > 0 ? Math.round(value) : null;
  }

  private parseNumber(value: string | null): number | undefined {
    if (!value) {
      return undefined;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private trimOrUndefined(value: string | null): string | undefined {
    if (!value) {
      return undefined;
    }
    const trimmed = value.trim();
    return trimmed ? trimmed : undefined;
  }

  private prettyLabel(value?: string): string {
    if (!value) {
      return '';
    }
    return value
      .split('-')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}