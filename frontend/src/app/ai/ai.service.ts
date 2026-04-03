import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { CatalogProductCard } from '../product/product-api.service';

export interface AiOrderPreview {
  orderNumber: string;
  status: string;
  paymentStatus: string;
  placedAt?: string;
}

export interface AiChatResponse {
  language: string;
  headline: string;
  answer: string;
  intent: string;
  quickReplies: string[];
  products: CatalogProductCard[];
  orders: AiOrderPreview[];
  nextStep: string;
}

export interface AiSearchInterpretation {
  query?: string;
  category?: string;
  brand?: string;
  tag?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
  inStock?: boolean | null;
  sort?: string;
}

export interface AiVoiceSearchResponse {
  transcript: string;
  summary: string;
  confidence: string;
  interpretation: AiSearchInterpretation;
  products: CatalogProductCard[];
  followUps: string[];
}

export interface AiImageSearchResponse {
  summary: string;
  extractedHint?: string | null;
  confidence: string;
  palette: string[];
  products: CatalogProductCard[];
  followUps: string[];
}

export interface AiRecommendationResponse {
  personalized: boolean;
  headline: string;
  summary: string;
  signals: string[];
  products: CatalogProductCard[];
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly api = `${environment.apiBase}/ai`;

  constructor(private readonly http: HttpClient) {}

  chat(message: string, language = 'en'): Observable<AiChatResponse> {
    return this.http.post<AiChatResponse>(`${this.api}/chat`, { message, language }).pipe(
      map((response) => this.normalizeChatResponse(response))
    );
  }

  getRecommendations(): Observable<AiRecommendationResponse> {
    return this.http.get<AiRecommendationResponse>(`${this.api}/recommendations`).pipe(
      map((response) => ({
        ...response,
        products: (response.products || []).map((product) => this.normalizeProduct(product))
      }))
    );
  }

  voiceSearch(transcript: string): Observable<AiVoiceSearchResponse> {
    return this.http.post<AiVoiceSearchResponse>(`${this.api}/voice-search`, { transcript }).pipe(
      map((response) => ({
        ...response,
        products: (response.products || []).map((product) => this.normalizeProduct(product))
      }))
    );
  }

  imageSearch(file: File, hint?: string): Observable<AiImageSearchResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (hint?.trim()) {
      formData.append('hint', hint.trim());
    }
    return this.http.post<AiImageSearchResponse>(`${this.api}/image-search`, formData).pipe(
      map((response) => ({
        ...response,
        products: (response.products || []).map((product) => this.normalizeProduct(product))
      }))
    );
  }

  private normalizeChatResponse(response: AiChatResponse): AiChatResponse {
    return {
      ...response,
      language: response.language || 'en',
      products: (response.products || []).map((product) => this.normalizeProduct(product)),
      quickReplies: response.quickReplies || [],
      orders: response.orders || []
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
}
