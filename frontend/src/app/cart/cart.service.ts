import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface FreeShippingInfo {
  thresholdCents: number;
  currentSubtotalCents: number;
  amountNeededCents: number;
  isEligible: boolean;
  progressPercentage: number;
  formattedThreshold: string;
  formattedCurrentTotal: string;
  formattedAmountNeeded: string;
}

export interface CartTotals {
  itemCount: number;
  subtotalCents: number;
  taxCents: number;
  shippingCents: number;
  discountCents: number;
  totalCents: number;
  currency: string;
  freeShippingInfo?: FreeShippingInfo;
}

export interface CartLineItem {
  itemId: number;
  productId: number;
  variantId: number;
  slug: string;
  title: string;
  coverImage?: string;
  categoryName?: string;
  brandName?: string;
  variantName?: string;
  sku: string;
  unitPriceCents: number;
  compareAtCents?: number | null;
  currency: string;
  quantity: number;
  stockQty: number;
  inStock: boolean;
  backorderAllowed: boolean;
  lowStock: boolean;
  stockLabel: string;
  purchasable: boolean;
  stockMessage: string;
  lineTotalCents: number;
}

export interface CartResponse {
  cartId: number;
  status: string;
  items: CartLineItem[];
  totals: CartTotals;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly api = `${environment.apiBase}/cart`;
  private readonly cartSubject = new BehaviorSubject<CartResponse | null>(null);

  readonly cart$ = this.cartSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get snapshot(): CartResponse | null {
    return this.cartSubject.value;
  }

  loadCart(): Observable<CartResponse> {
    return this.http.get<CartResponse>(this.api).pipe(tap((cart) => this.cartSubject.next(cart)));
  }

  addItem(sku: string, quantity = 1): Observable<CartResponse> {
    return this.http.post<CartResponse>(`${this.api}/items`, { sku, quantity }).pipe(tap((cart) => this.cartSubject.next(cart)));
  }

  updateQuantity(itemId: number, quantity: number): Observable<CartResponse> {
    return this.http.patch<CartResponse>(`${this.api}/items/${itemId}`, { quantity }).pipe(tap((cart) => this.cartSubject.next(cart)));
  }

  removeItem(itemId: number): Observable<CartResponse> {
    return this.http.delete<CartResponse>(`${this.api}/items/${itemId}`).pipe(tap((cart) => this.cartSubject.next(cart)));
  }

  clear(): Observable<CartResponse> {
    return this.http.delete<CartResponse>(this.api).pipe(tap((cart) => this.cartSubject.next(cart)));
  }
}