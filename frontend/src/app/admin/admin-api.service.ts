import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminDashboard {
  totalUsers: number;
  totalProducts: number;
  totalOrders: number;
  newOrders: number;
  deliveredOrders: number;
  orderStatusCounts: Record<string, number>;
}

export interface AdminForecastOverview {
  trailingRevenueCents: number;
  trailingOrders: number;
  projectedRevenueCents: number;
  projectedOrders: number;
  revenueGrowthRate: number;
  pendingOrders: number;
  lowStockRiskCount: number;
}

export interface AdminForecastPoint {
  label: string;
  value: number;
}

export interface AdminForecastCategory {
  category: string;
  slug: string;
  unitsSold: number;
  projectedUnits: number;
  trendRate: number;
}

export interface AdminForecastRisk {
  title: string;
  slug: string;
  stockQty: number;
  lowStockThreshold: number;
  soldLast30Days: number;
  guidance: string;
}

export interface AdminForecast {
  overview: AdminForecastOverview;
  revenueTrend: AdminForecastPoint[];
  orderTrend: AdminForecastPoint[];
  categoryDemand: AdminForecastCategory[];
  inventoryRisks: AdminForecastRisk[];
  actions: string[];
}

export interface AdminUser {
  id: number;
  email: string;
  phone?: string;
  role: string;
  status: string;
  createdAt: string;
  lastLoginAt?: string;
}

export interface AdminOrder {
  id: number;
  orderNumber: string;
  userEmail: string;
  status: string;
  totalCents: number;
  paymentStatus: string;
  placedAt: string;
}

export interface AdminOrderLine {
  title: string;
  sku: string;
  unitPriceCents: number;
  quantity: number;
  lineTotalCents: number;
  coverImage?: string;
  productSlug?: string;
}

export interface AdminStatusEvent {
  code: string;
  label: string;
  at: string;
  detail: string;
}

export interface AdminOrderPayment {
  provider: string;
  status: string;
  providerOrderId?: string;
  providerPaymentId?: string;
  checkoutLabel: string;
  requiresAction: boolean;
  publicKey?: string;
}

export interface AdminOrderDetail {
  summary: {
    orderNumber: string;
    status: string;
    paymentStatus: string;
    totalCents: number;
    currency: string;
    placedAt: string;
    itemCount: number;
  };
  items: AdminOrderLine[];
  timeline: AdminStatusEvent[];
  shippingAddress: string;
  billingAddress: string;
  payment?: AdminOrderPayment | null;
}

export interface AdminBrand {
  id: number;
  name: string;
  slug: string;
}

export interface AdminBrandPayload {
  name: string;
}

export interface AdminProductMedia {
  id?: number;
  url: string;
  altText?: string;
  sortOrder?: number;
}

export interface AdminProduct {
  id: number;
  title: string;
  slug: string;
  description?: string;
  coverImage?: string;
  status: string;
  categoryName?: string;
  brandName?: string;
  sku?: string;
  variantName?: string;
  tags: string[];
  media: AdminProductMedia[];
  priceCents: number;
  stockQty: number;
  createdAt: string;
}

export interface AdminProductPayload {
  title: string;
  slug?: string;
  description?: string;
  coverImage?: string;
  categoryName?: string;
  brandName?: string;
  sku?: string;
  variantName?: string;
  status?: string;
  tags?: string[];
  media?: AdminProductMedia[];
  priceCents: number;
  stockQty: number;
}

export interface AdminCreateAdminPayload {
  email: string;
  password: string;
  phone?: string;
}

export interface AdminUploadResponse {
  url: string;
  fileName: string;
}

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private api = `${environment.apiBase}/admin`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.api}/dashboard`);
  }

  getForecast(): Observable<AdminForecast> {
    return this.http.get<AdminForecast>(`${this.api}/forecast`);
  }

  getUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.api}/users`);
  }

  getAdmins(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.api}/admins`);
  }

  createAdmin(body: AdminCreateAdminPayload): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${this.api}/admins`, body);
  }

  getBrands(): Observable<AdminBrand[]> {
    return this.http.get<AdminBrand[]>(`${this.api}/brands`);
  }

  createBrand(body: AdminBrandPayload): Observable<AdminBrand> {
    return this.http.post<AdminBrand>(`${this.api}/brands`, body);
  }

  getOrders(): Observable<AdminOrder[]> {
    return this.http.get<AdminOrder[]>(`${this.api}/orders`);
  }

  getOrderDetail(orderId: number): Observable<AdminOrderDetail> {
    return this.http.get<AdminOrderDetail>(`${this.api}/orders/${orderId}`);
  }

  updateOrderStatus(orderId: number, status: string): Observable<AdminOrder> {
    return this.http.patch<AdminOrder>(`${this.api}/orders/${orderId}/status`, { status });
  }

  getProducts(): Observable<AdminProduct[]> {
    return this.http.get<AdminProduct[]>(`${this.api}/products`).pipe(
      map((products) => products.map((product) => this.normalizeProduct(product)))
    );
  }

  getProduct(productId: number): Observable<AdminProduct> {
    return this.http.get<AdminProduct>(`${this.api}/products/${productId}`).pipe(
      map((product) => this.normalizeProduct(product))
    );
  }

  createProduct(body: AdminProductPayload): Observable<AdminProduct> {
    return this.http.post<AdminProduct>(`${this.api}/products`, body).pipe(
      map((product) => this.normalizeProduct(product))
    );
  }

  updateProduct(productId: number, body: AdminProductPayload): Observable<AdminProduct> {
    return this.http.put<AdminProduct>(`${this.api}/products/${productId}`, body).pipe(
      map((product) => this.normalizeProduct(product))
    );
  }

  deleteProduct(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/products/${productId}`);
  }

  uploadProductImage(file: File): Observable<AdminUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AdminUploadResponse>(`${this.api}/uploads/product-image`, formData).pipe(
      map((res) => ({
        ...res,
        url: this.normalizeImageUrl(res.url)
      }))
    );
  }

  private normalizeProduct(product: AdminProduct): AdminProduct {
    const media = (product.media || []).map((item, index) => ({
      ...item,
      url: this.normalizeImageUrl(item.url),
      sortOrder: item.sortOrder ?? index
    }));
    const coverImage = this.normalizeImageUrl(product.coverImage || media[0]?.url || '');

    return {
      ...product,
      coverImage,
      tags: product.tags || [],
      media
    };
  }

  private normalizeImageUrl(url: string): string {
    if (!url) {
      return url;
    }
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('assets/')) {
      return url;
    }
    return `${environment.apiBase}${url.startsWith('/') ? '' : '/'}${url}`;
  }
}