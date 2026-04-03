import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { environment } from '../../environments/environment';
import { AddressSummary, OrderSummary, ProfileSummary } from '../order/order.service';

interface CheckoutProfileResponse {
  profile: ProfileSummary;
  addresses: AddressSummary[];
}

interface UserDashboardResponse {
  profile: ProfileSummary;
  addresses: AddressSummary[];
  orders: OrderSummary[];
}

@Component({
  selector: 'app-user-home',
  templateUrl: './user-home.component.html',
  styleUrls: ['./user-home.component.scss']
})
export class UserHomeComponent implements OnInit {
  profile: ProfileSummary | null = null;
  addresses: AddressSummary[] = [];
  orders: OrderSummary[] = [];
  loading = true;
  error = '';
  notice = '';
  busyAddressId: number | null = null;

  private readonly usersApi = `${environment.apiBase}/users/me`;
  private readonly ordersApi = `${environment.apiBase}/orders`;
  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  get displayName(): string {
    const first = (this.profile?.firstName || '').trim();
    const last = (this.profile?.lastName || '').trim();
    const fullName = `${first} ${last}`.trim();
    if (fullName) {
      return fullName;
    }

    const email = (this.profile?.email || '').trim();
    if (!email) {
      return 'Your account';
    }

    return email.split('@')[0]
      .split(/[._-]+/)
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  get initials(): string {
    const pieces = this.displayName.split(' ').filter(Boolean).slice(0, 2);
    if (!pieces.length) {
      return 'NB';
    }
    return pieces.map((piece) => piece.charAt(0).toUpperCase()).join('');
  }

  get sortedAddresses(): AddressSummary[] {
    return [...this.addresses].sort((left, right) => Number(right.isDefault) - Number(left.isDefault));
  }

  get defaultAddress(): AddressSummary | undefined {
    return this.sortedAddresses.find((address) => address.isDefault) || this.sortedAddresses[0];
  }

  get recentOrders(): OrderSummary[] {
    return [...this.orders]
      .sort((left, right) => new Date(right.placedAt || 0).getTime() - new Date(left.placedAt || 0).getTime())
      .slice(0, 4);
  }

  get totalSpentCents(): number {
    return this.orders
      .filter((order) => !this.isExcludedFromSpend(order))
      .reduce((sum, order) => sum + (order.totalCents ?? 0), 0);
  }
  get activeOrdersCount(): number {
    return this.orders.filter((order) => !['delivered', 'cancelled', 'failed'].includes(this.normalize(order.status))).length;
  }

  get completedOrdersCount(): number {
    return this.orders.filter((order) => this.statusTone(order.paymentStatus) === 'success').length;
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '';
  }

  statusTone(value?: string): 'success' | 'warning' | 'danger' | 'neutral' {
    const normalized = this.normalize(value);
    if (['success', 'captured', 'authorized', 'paid', 'delivered', 'completed', 'refunded'].includes(normalized)) {
      return 'success';
    }
    if (['failed', 'declined', 'cancelled'].includes(normalized)) {
      return 'danger';
    }
    if (['pending', 'initiated', 'processing', 'created', 'packed', 'in_transit', 'shipped', 'refund_pending'].includes(normalized)) {
      return 'warning';
    }
    return 'neutral';
  }

  prettify(value?: string): string {
    return (value || 'Unknown')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  trackByAddress(_: number, address: AddressSummary): number {
    return address.id;
  }

  trackByOrder(_: number, order: OrderSummary): string {
    return order.orderNumber;
  }

  private isExcludedFromSpend(order: OrderSummary): boolean {
    const status = this.normalize(order.status);
    const paymentStatus = this.normalize(order.paymentStatus);
    return status === 'cancelled' || ['refunded', 'refund_pending', 'cancelled'].includes(paymentStatus);
  }

  setDefault(address: AddressSummary): void {
    if (address.isDefault || this.busyAddressId === address.id) {
      return;
    }

    this.busyAddressId = address.id;
    this.notice = '';
    this.error = '';

    this.http.patch<AddressSummary>(`${this.usersApi}/addresses/${address.id}/default`, {}).subscribe({
      next: (updated: AddressSummary) => {
        this.addresses = this.addresses.map((item) => ({
          ...item,
          isDefault: item.id === updated.id
        }));
        this.notice = `${updated.label || 'Selected address'} is now your default delivery address.`;
        this.busyAddressId = null;
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.error?.message || 'Could not update the default address right now.';
        this.busyAddressId = null;
      }
    });
  }

  private loadDashboard(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      profilePayload: this.http.get<CheckoutProfileResponse>(`${this.usersApi}/checkout-profile`),
      orders: this.http.get<OrderSummary[]>(`${this.ordersApi}/me`)
    }).subscribe({
      next: ({ profilePayload, orders }: { profilePayload: CheckoutProfileResponse; orders: OrderSummary[] }) => {
        const dashboard: UserDashboardResponse = {
          profile: profilePayload.profile,
          addresses: profilePayload.addresses ?? [],
          orders: orders ?? []
        };
        this.profile = dashboard.profile;
        this.addresses = dashboard.addresses;
        this.orders = dashboard.orders;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.error?.message || 'Could not load your account right now.';
        this.loading = false;
      }
    });
  }

  private normalize(value?: string): string {
    return (value || '').trim().toLowerCase();
  }
}