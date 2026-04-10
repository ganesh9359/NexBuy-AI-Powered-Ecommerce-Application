import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CatalogCategory {
  id: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  productCount: number;
}

export interface CatalogBrand {
  id: number;
  name: string;
  slug: string;
  productCount: number;
}

export interface CatalogTag {
  tag: string;
  productCount: number;
}

export interface CatalogPriceRange {
  minCents?: number | null;
  maxCents?: number | null;
}

export interface CatalogFilters {
  categories: CatalogCategory[];
  brands: CatalogBrand[];
  tags: CatalogTag[];
  priceRange: CatalogPriceRange;
}

export interface CatalogMedia {
  id?: number;
  url: string;
  altText?: string;
  sortOrder?: number;
}

export interface CatalogStock {
  stockQty: number;
  lowStockThreshold: number;
  backorderAllowed: boolean;
  inStock: boolean;
  lowStock: boolean;
  label: string;
}

export interface CatalogProductCard {
  id: number;
  slug: string;
  title: string;
  description?: string;
  coverImage?: string;
  categoryName?: string;
  categorySlug?: string;
  brandName?: string;
  brandSlug?: string;
  sku?: string;
  variantName?: string;
  priceCents: number;
  compareAtCents?: number | null;
  currency?: string;
  stock: CatalogStock;
  tags: string[];
  media: CatalogMedia[];
}

export interface CatalogStat {
  label: string;
  value: string;
}

export interface CatalogHomeResponse {
  stats: CatalogStat[];
  spotlightProduct?: CatalogProductCard | null;
  featuredProducts: CatalogProductCard[];
  valuePicks: CatalogProductCard[];
  categories: CatalogCategory[];
  trendingTags: CatalogTag[];
}

export interface CatalogListingResponse {
  items: CatalogProductCard[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  query?: string;
  category?: string;
  brand?: string;
  tag?: string;
  sort: string;
  filters: CatalogFilters;
}

export interface ProductDetailResponse {
  product: CatalogProductCard;
  relatedProducts: CatalogProductCard[];
}

export interface CatalogQuery {
  q?: string;
  category?: string;
  brand?: string;
  tag?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  sort?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  private readonly api = `${environment.apiBase}/products`;

  constructor(private http: HttpClient) {}

  getHome(limit = 8): Observable<CatalogHomeResponse> {
    return this.http.get<CatalogHomeResponse>(`${this.api}/home`, { params: this.toParams({ limit }) }).pipe(
      map((home) => this.normalizeHome(home))
    );
  }

  getCategories(): Observable<CatalogCategory[]> {
    return this.http.get<CatalogCategory[]>(`${this.api}/categories`).pipe(
      map((categories) => (categories || []).map((category) => this.normalizeCategory(category)))
    );
  }

  getCatalog(query: CatalogQuery = {}): Observable<CatalogListingResponse> {
    return this.http.get<CatalogListingResponse>(`${this.api}/catalog`, { params: this.toParams({ ...query }) }).pipe(
      map((response) => this.normalizeListing(response))
    );
  }

  search(query: CatalogQuery): Observable<CatalogListingResponse> {
    return this.http.get<CatalogListingResponse>(`${this.api}/search`, { params: this.toParams({ ...query }) }).pipe(
      map((response) => this.normalizeListing(response))
    );
  }

  getSearchSuggestions(query: string, limit = 10): Observable<string[]> {
    return this.http.get<string[]>(`${this.api}/search/suggestions`, { 
      params: this.toParams({ q: query, limit }) 
    });
  }

  getProduct(slug: string, relatedLimit = 4): Observable<ProductDetailResponse> {
    return this.http.get<ProductDetailResponse>(`${this.api}/${encodeURIComponent(slug)}`, {
      params: this.toParams({ relatedLimit })
    }).pipe(
      map((response) => ({
        product: this.normalizeProduct(response.product),
        relatedProducts: (response.relatedProducts || []).map((item) => this.normalizeProduct(item))
      }))
    );
  }

  private normalizeHome(home: CatalogHomeResponse): CatalogHomeResponse {
    return {
      ...home,
      spotlightProduct: home.spotlightProduct ? this.normalizeProduct(home.spotlightProduct) : null,
      featuredProducts: (home.featuredProducts || []).map((item) => this.normalizeProduct(item)),
      valuePicks: (home.valuePicks || []).map((item) => this.normalizeProduct(item)),
      categories: (home.categories || []).map((category) => this.normalizeCategory(category)),
      trendingTags: home.trendingTags || []
    };
  }

  private normalizeListing(response: CatalogListingResponse): CatalogListingResponse {
    return {
      ...response,
      items: (response.items || []).map((item) => this.normalizeProduct(item)),
      filters: {
        categories: (response.filters?.categories || []).map((category) => this.normalizeCategory(category)),
        brands: response.filters?.brands || [],
        tags: response.filters?.tags || [],
        priceRange: response.filters?.priceRange || {}
      }
    };
  }

  private normalizeCategory(category: CatalogCategory): CatalogCategory {
    return {
      ...category,
      imageUrl: this.normalizeImageUrl(category.imageUrl)
    };
  }

  private normalizeProduct(product: CatalogProductCard): CatalogProductCard {
    const media = (product.media || []).map((item, index) => ({
      ...item,
      url: this.normalizeImageUrl(item.url),
      sortOrder: item.sortOrder ?? index
    }));
    const coverImage = this.normalizeImageUrl(product.coverImage || media[0]?.url || '');

    return {
      ...product,
      coverImage,
      media,
      tags: product.tags || [],
      stock: product.stock || {
        stockQty: 0,
        lowStockThreshold: 0,
        backorderAllowed: false,
        inStock: false,
        lowStock: false,
        label: 'Out of stock'
      }
    };
  }

  private normalizeImageUrl(url?: string): string {
    if (!url) {
      return '';
    }
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('assets/')) {
      return url;
    }
    return `${environment.apiBase}${url.startsWith('/') ? '' : '/'}${url}`;
  }

  private toParams(query: Record<string, string | number | boolean | undefined | null>): HttpParams {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') {
        return;
      }
      params = params.set(key, String(value));
    });
    return params;
  }
}

