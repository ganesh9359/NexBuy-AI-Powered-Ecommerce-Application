import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminApiService, AdminOrder, AdminOrderDetail } from './admin-api.service';

@Component({
  selector: 'app-admin-orders',
  templateUrl: './admin-orders.component.html',
  styleUrls: ['./admin-orders.component.scss']
})
export class AdminOrdersComponent implements OnInit {
  orders: AdminOrder[] = [];
  loading = true;
  savingId: number | null = null;
  detailsLoadingId: number | null = null;
  expandedOrderId: number | null = null;
  query = '';
  statusFilter = 'ALL';
  error = '';
  detailError = '';
  draftStatuses: Record<number, string> = {};
  orderDetails: Record<number, AdminOrderDetail> = {};
  readonly statuses = ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'FAILED'];
  readonly filterStatuses = ['ALL', 'PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'FAILED'];

  constructor(
    private adminApi: AdminApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const requestedStatus = (params.get('status') || 'ALL').toUpperCase();
      this.statusFilter = this.filterStatuses.includes(requestedStatus) ? requestedStatus : 'ALL';
    });
    this.loadOrders();
  }

  get filteredOrders(): AdminOrder[] {
    const term = this.query.trim().toLowerCase();
    return this.orders.filter((order) => {
      const matchesQuery = !term || [order.orderNumber, order.userEmail, order.status, order.paymentStatus].some((value) =>
        value.toLowerCase().includes(term)
      );
      const matchesStatus = this.statusFilter === 'ALL' || order.status.toUpperCase() === this.statusFilter;
      return matchesQuery && matchesStatus;
    });
  }

  statusFor(order: AdminOrder): string {
    return this.draftStatuses[order.id] || order.status;
  }

  canEditStatus(order: AdminOrder): boolean {
    const status = order.status.toUpperCase();
    return status !== 'DELIVERED' && status !== 'CANCELLED';
  }

  isExpanded(order: AdminOrder): boolean {
    return this.expandedOrderId === order.id;
  }

  setStatusFilter(status: string): void {
    const normalized = (status || 'ALL').toUpperCase();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { status: normalized === 'ALL' ? null : normalized },
      queryParamsHandling: 'merge'
    });
  }

  toggleDetails(order: AdminOrder): void {
    if (this.isExpanded(order)) {
      this.expandedOrderId = null;
      this.detailError = '';
      return;
    }

    this.expandedOrderId = order.id;
    this.detailError = '';
    if (this.orderDetails[order.id]) {
      return;
    }

    this.detailsLoadingId = order.id;
    this.adminApi.getOrderDetail(order.id).subscribe({
      next: (detail) => {
        this.orderDetails[order.id] = detail;
        this.detailsLoadingId = null;
      },
      error: () => {
        this.detailsLoadingId = null;
        this.detailError = 'Could not load order details right now.';
      }
    });
  }

  saveStatus(order: AdminOrder): void {
    if (!this.canEditStatus(order)) {
      return;
    }

    const nextStatus = this.statusFor(order);
    this.savingId = order.id;
    this.error = '';
    this.adminApi.updateOrderStatus(order.id, nextStatus.toLowerCase()).subscribe({
      next: (updated) => {
        this.orders = this.orders.map((item) => (item.id === updated.id ? updated : item));
        delete this.draftStatuses[order.id];
        this.savingId = null;
      },
      error: (err) => {
        this.savingId = null;
        this.error = err?.error?.message || 'Could not update the order status.';
      }
    });
  }

  formatPrice(totalCents: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(totalCents / 100);
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '';
  }

  private loadOrders(): void {
    this.loading = true;
    this.error = '';
    this.adminApi.getOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Could not load admin orders.';
      }
    });
  }
}