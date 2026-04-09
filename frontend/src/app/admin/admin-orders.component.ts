import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AdminApiService, AdminOrder, AdminOrderDetail } from './admin-api.service';
import { ReturnRequest } from './admin-returns.component';
import { environment } from '../../environments/environment';

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
  returnReviewingId: number | null = null;
  draftStatuses: Record<number, string> = {};
  orderDetails: Record<number, AdminOrderDetail> = {};

  // Return requests state
  returns: ReturnRequest[] = [];
  returnedOrderIds = new Set<number>();
  returnsLoading = false;
  returnsError = '';
  returnsActionInProgress = false;
  returnsSuccessMsg = '';
  selectedReturn: ReturnRequest | null = null;

  readonly statuses = ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'FAILED'];
  readonly filterStatuses = ['ALL', 'PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'FAILED', 'RETURNS'];

  readonly returnActionMap: Record<string, { label: string; action: string; cls: string }[]> = {
    requested: [
      { label: 'Approve', action: 'approve', cls: 'approve-btn' },
      { label: 'Reject',  action: 'reject',  cls: 'reject-btn'  }
    ],
    approved: [
      { label: 'Mark Picked Up', action: 'accept', cls: 'approve-btn' },
      { label: 'Cancel',         action: 'cancel', cls: 'ghost-btn'   }
    ],
    accepted: [
      { label: 'Mark Completed', action: 'complete', cls: 'approve-btn' }
    ]
  };

  private readonly returnsApi = `${environment.apiBase}/admin/returns`;

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', minimumFractionDigits: 2
  });

  constructor(
    private adminApi: AdminApiService,
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const requestedStatus = (params.get('status') || 'ALL').toUpperCase();
      this.statusFilter = this.filterStatuses.includes(requestedStatus) ? requestedStatus : 'ALL';
      if (this.statusFilter === 'RETURNS' && this.returns.length === 0) {
        this.loadReturns();
      }
    });
    this.loadOrders();
    this.loadReturns();
  }

  get showReturns(): boolean {
    return this.statusFilter === 'RETURNS';
  }

  get filteredOrders(): AdminOrder[] {
    const term = this.query.trim().toLowerCase();
    return this.orders.filter((order) => {
      const matchesQuery = !term || [order.orderNumber, order.userEmail, order.status, order.paymentStatus]
        .some((v) => v.toLowerCase().includes(term));
      const matchesStatus = this.statusFilter === 'ALL' || order.status.toUpperCase() === this.statusFilter;
      return matchesQuery && matchesStatus;
    });
  }

  get filteredReturns(): ReturnRequest[] {
    const term = this.query.trim().toLowerCase();
    return this.returns.filter((r) =>
      !term ||
      r.orderNumber.toLowerCase().includes(term) ||
      r.orderId.toString().includes(term) ||
      r.userId.toString().includes(term) ||
      (r.reason || '').toLowerCase().includes(term)
    );
  }

  // ── Orders ──────────────────────────────────────────────

  statusFor(order: AdminOrder): string {
    return this.draftStatuses[order.id] || order.status;
  }

  canEditStatus(order: AdminOrder): boolean {
    const s = order.status.toUpperCase();
    return s !== 'DELIVERED' && s !== 'CANCELLED';
  }

  canReviewReturn(detail?: AdminOrderDetail | null): boolean {
    return (detail?.returnRequest?.status || '').toUpperCase() === 'REQUESTED' && this.returnReviewingId === null;
  }

  isExpanded(order: AdminOrder): boolean {
    return this.expandedOrderId === order.id;
  }

  setStatusFilter(status: string): void {
    const normalized = (status || 'ALL').toUpperCase();
    if (normalized === 'RETURNS' && this.returns.length === 0) {
      this.loadReturns();
    }
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
    if (this.orderDetails[order.id]) return;

    this.detailsLoadingId = order.id;
    this.adminApi.getOrderDetail(order.id).subscribe({
      next: (detail) => { this.orderDetails[order.id] = detail; this.detailsLoadingId = null; },
      error: () => { this.detailsLoadingId = null; this.detailError = 'Could not load order details right now.'; }
    });
  }

  saveStatus(order: AdminOrder): void {
    if (!this.canEditStatus(order)) return;
    const nextStatus = this.statusFor(order);
    this.savingId = order.id;
    this.error = '';
    this.adminApi.updateOrderStatus(order.id, nextStatus.toLowerCase()).subscribe({
      next: (updated) => {
        this.orders = this.orders.map((item) => item.id === updated.id ? updated : item);
        delete this.draftStatuses[order.id];
        this.savingId = null;
      },
      error: (err) => { this.savingId = null; this.error = err?.error?.message || 'Could not update the order status.'; }
    });
  }

  reviewReturn(order: AdminOrder, action: 'approve' | 'reject'): void {
    const detail = this.orderDetails[order.id];
    if (!this.canReviewReturn(detail)) return;
    if (!window.confirm(action === 'approve' ? 'Approve this return and start the refund flow?' : 'Reject this return request?')) return;

    this.returnReviewingId = order.id;
    this.detailError = '';
    this.adminApi.reviewReturn(order.id, action).subscribe({
      next: (updatedDetail) => {
        this.orderDetails[order.id] = updatedDetail;
        this.orders = this.orders.map((item) => item.id === order.id
          ? { ...item, paymentStatus: updatedDetail.summary.paymentStatus, status: updatedDetail.summary.status }
          : item);
        this.returnReviewingId = null;
      },
      error: (err) => { this.returnReviewingId = null; this.detailError = err?.error?.message || 'Could not review this return request.'; }
    });
  }

  // ── Return Requests ──────────────────────────────────────

  loadReturns(): void {
    this.returnsLoading = true;
    this.returnsError = '';
    this.http.get<ReturnRequest[]>(this.returnsApi).subscribe({
      next: (data) => {
        this.returns = data;
        this.returnedOrderIds = new Set(data.map(r => r.orderId));
        this.returnsLoading = false;
      },
      error: (err) => { this.returnsError = err?.error?.message || 'Could not load return requests.'; this.returnsLoading = false; }
    });
  }

  returnActionsFor(status: string): { label: string; action: string; cls: string }[] {
    return this.returnActionMap[status?.toLowerCase()] || [];
  }

  selectReturn(ret: ReturnRequest): void {
    this.selectedReturn = this.selectedReturn?.id === ret.id ? null : ret;
  }

  closeReturnDetail(): void {
    this.selectedReturn = null;
  }

  updateReturnStatus(returnId: number, action: string): void {
    this.returnsActionInProgress = true;
    this.returnsError = '';
    this.returnsSuccessMsg = '';
    this.http.patch<ReturnRequest>(`${this.returnsApi}/${returnId}`, { action }).subscribe({
      next: (updated) => {
        const idx = this.returns.findIndex((r) => r.id === returnId);
        if (idx !== -1) this.returns[idx] = updated;
        if (this.selectedReturn?.id === returnId) this.selectedReturn = updated;
        this.returnsSuccessMsg = `Return #${returnId} updated to "${this.prettify(updated.status)}".`;
        this.returnsActionInProgress = false;
        setTimeout(() => (this.returnsSuccessMsg = ''), 3500);
      },
      error: (err) => { this.returnsError = err?.error?.message || `Could not perform "${action}".`; this.returnsActionInProgress = false; }
    });
  }

  // ── Shared helpers ───────────────────────────────────────

  formatPrice(cents: number): string {
    return this.currency.format(cents / 100);
  }

  formatReturnPrice(amount?: number): string {
    return this.currency.format(amount ?? 0);
  }

  formatDate(value?: string | null): string {
    return value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—';
  }

  prettify(value?: string | null): string {
    return (value || 'Unknown').replace(/_/g, ' ').replace(/\b\w/g, (l) => l.toUpperCase());
  }

  tone(status?: string | null): string {
    const s = status?.toLowerCase() || '';
    if (['approved', 'accepted', 'completed', 'processed'].includes(s)) return 'success';
    if (['rejected', 'cancelled', 'failed'].includes(s)) return 'danger';
    if (['requested', 'pending', 'processing'].includes(s)) return 'warning';
    return 'neutral';
  }

  private loadOrders(): void {
    this.loading = true;
    this.error = '';
    this.adminApi.getOrders().subscribe({
      next: (orders) => { this.orders = orders; this.loading = false; },
      error: () => { this.loading = false; this.error = 'Could not load admin orders.'; }
    });
  }
}
