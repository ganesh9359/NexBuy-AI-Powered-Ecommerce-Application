import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CartResponse } from '../cart/cart.service';

export interface ProfileSummary {
  userId: number;
  email: string;
  firstName?: string;
  lastName?: string;
}

export interface AddressSummary {
  id: number;
  label?: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
  displayText: string;
}

export interface CheckoutViewResponse {
  profile: ProfileSummary;
  addresses: AddressSummary[];
  cart: CartResponse;
  availableProviders: string[];
  recommendedProvider: string;
}

export interface PaymentIntent {
  provider: string;
  status: string;
  providerOrderId?: string;
  providerPaymentId?: string;
  checkoutLabel: string;
  requiresAction: boolean;
  publicKey?: string;
}

export interface RefundSummary {
  status: string;
  amountCents: number;
  currency: string;
  providerRefundId?: string;
  note?: string;
  requestedAt: string;
  processedAt?: string;
}

export interface ReturnSummary {
  status: string;
  refundStatus: string;
  reason?: string;
  requestedAt: string;
  reviewedAt?: string;
  updatedAt: string;
}

export interface OrderSummary {
  orderNumber: string;
  status: string;
  paymentStatus: string;
  totalCents: number;
  currency: string;
  placedAt: string;
  itemCount: number;
}

export interface OrderLine {
  title: string;
  sku: string;
  unitPriceCents: number;
  quantity: number;
  lineTotalCents: number;
  coverImage?: string;
  productSlug?: string;
}

export interface StatusEvent {
  code: string;
  label: string;
  at: string;
  detail: string;
}

export interface OrderDetail {
  summary: OrderSummary;
  items: OrderLine[];
  timeline: StatusEvent[];
  shippingAddress: string;
  billingAddress: string;
  payment?: PaymentIntent | null;
  refund?: RefundSummary | null;
  returnRequest?: ReturnSummary | null;
}

export interface PlaceOrderResponse {
  order: OrderDetail;
  cart: CartResponse;
}

export interface CancelOrderResponse {
  removed: boolean;
  order?: OrderDetail | null;
}

export interface UpsertAddressRequest {
  label?: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault?: boolean;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly ordersApi = `${environment.apiBase}/orders`;
  private readonly usersApi = `${environment.apiBase}/users/me`;

  constructor(private readonly http: HttpClient) {}

  getCheckout(): Observable<CheckoutViewResponse> {
    return this.http.get<CheckoutViewResponse>(`${this.ordersApi}/checkout`);
  }

  placeOrder(body: { shippingAddressId: number; billingAddressId?: number | null; billingSameAsShipping?: boolean; paymentProvider: string }): Observable<PlaceOrderResponse> {
    return this.http.post<PlaceOrderResponse>(`${this.ordersApi}/checkout`, body);
  }

  getOrders(): Observable<OrderSummary[]> {
    return this.http.get<OrderSummary[]>(`${this.ordersApi}/me`);
  }

  getOrder(orderNumber: string): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${this.ordersApi}/${encodeURIComponent(orderNumber)}`);
  }

  cancelOrder(orderNumber: string): Observable<CancelOrderResponse> {
    return this.http.post<CancelOrderResponse>(`${this.ordersApi}/${encodeURIComponent(orderNumber)}/cancel`, {});
  }

  requestReturn(orderNumber: string, body?: { reason?: string }): Observable<OrderDetail> {
    return this.http.post<OrderDetail>(`${this.ordersApi}/${encodeURIComponent(orderNumber)}/return`, body ?? {});
  }

  createAddress(body: UpsertAddressRequest): Observable<AddressSummary> {
    return this.http.post<AddressSummary>(`${this.usersApi}/addresses`, body);
  }

  updateAddress(addressId: number, body: UpsertAddressRequest): Observable<AddressSummary> {
    return this.http.put<AddressSummary>(`${this.usersApi}/addresses/${addressId}`, body);
  }

  setDefaultAddress(addressId: number): Observable<AddressSummary> {
    return this.http.patch<AddressSummary>(`${this.usersApi}/addresses/${addressId}/default`, {});
  }
}