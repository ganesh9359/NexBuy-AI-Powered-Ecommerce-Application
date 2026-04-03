import { Component, OnInit } from '@angular/core';
import { OrderService, OrderSummary } from './order.service';

@Component({
  selector: 'app-order-home',
  templateUrl: './order-home.component.html',
  styleUrls: ['./order-home.component.scss']
})
export class OrderHomeComponent implements OnInit {
  orders: OrderSummary[] = [];
  loading = true;
  error = '';

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(private readonly orderService: OrderService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  get totalSpentCents(): number {
    return this.orders
      .filter((order) => !this.isExcludedFromSpend(order))
      .reduce((sum, order) => sum + (order.totalCents ?? 0), 0);
  }
  get completedOrdersCount(): number {
    return this.orders.filter((order) => this.statusTone(order.paymentStatus) === 'success').length;
  }

  get activeOrdersCount(): number {
    return this.orders.filter((order) => !['delivered', 'cancelled', 'failed'].includes(this.normalize(order.status))).length;
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
    if (['pending', 'initiated', 'processing', 'created', 'packed', 'in_transit', 'refund_pending'].includes(normalized)) {
      return 'warning';
    }
    return 'neutral';
  }

  prettify(value?: string): string {
    return (value || 'Unknown')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  private isExcludedFromSpend(order: OrderSummary): boolean {
    const status = this.normalize(order.status);
    const paymentStatus = this.normalize(order.paymentStatus);
    return status === 'cancelled' || ['refunded', 'refund_pending', 'cancelled'].includes(paymentStatus);
  }

  private loadOrders(): void {
    this.loading = true;
    this.error = '';
    this.orderService.getOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load your orders.';
        this.loading = false;
      }
    });
  }

  private normalize(value?: string): string {
    return (value || '').trim().toLowerCase();
  }
}