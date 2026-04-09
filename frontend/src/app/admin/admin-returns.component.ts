import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface ReturnRequest {
  id: number;
  orderId: number;
  userId: number;
  orderNumber: string;
  status: string;
  refundStatus: string;
  reason: string;
  refundAmountInr: number;
  requestedAt: string;
  reviewedAt: string | null;
  pickedAt: string | null;
  updatedAt: string;
}

@Component({
  selector: 'app-admin-returns',
  templateUrl: './admin-returns.component.html',
  styleUrls: ['./admin-returns.component.scss']
})
export class AdminReturnsComponent implements OnInit {
  returns: ReturnRequest[] = [];
  filteredReturns: ReturnRequest[] = [];
  loading = true;
  error = '';
  successMsg = '';
  selectedStatus = '';
  searchTerm = '';
  selectedReturn: ReturnRequest | null = null;
  actionInProgress = false;

  private readonly api = `${environment.apiBase}/admin/returns`;

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  readonly statusOptions = ['requested', 'approved', 'accepted', 'rejected', 'completed', 'cancelled'];

  readonly actionMap: Record<string, { label: string; action: string; cls: string }[]> = {
    requested: [
      { label: 'Approve', action: 'approve', cls: 'btn-success' },
      { label: 'Reject',  action: 'reject',  cls: 'btn-danger'  }
    ],
    approved: [
      { label: 'Mark Picked Up', action: 'accept', cls: 'btn-warning' },
      { label: 'Cancel',         action: 'cancel', cls: 'btn-ghost'   }
    ],
    accepted: [
      { label: 'Mark Completed', action: 'complete', cls: 'btn-success' }
    ]
  };

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.loadReturns();
  }

  loadReturns(): void {
    this.loading = true;
    this.error = '';
    this.http.get<ReturnRequest[]>(this.api).subscribe({
      next: (data) => {
        this.returns = data;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load return requests.';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    const term = this.searchTerm.toLowerCase();
    this.filteredReturns = this.returns.filter((r) => {
      const statusMatch = !this.selectedStatus || r.status === this.selectedStatus;
      const searchMatch = !term ||
        r.orderNumber.toLowerCase().includes(term) ||
        r.orderId.toString().includes(term) ||
        r.userId.toString().includes(term) ||
        (r.reason || '').toLowerCase().includes(term);
      return statusMatch && searchMatch;
    });
  }

  selectReturn(ret: ReturnRequest): void {
    this.selectedReturn = this.selectedReturn?.id === ret.id ? null : ret;
  }

  closeDetail(): void {
    this.selectedReturn = null;
  }

  actionsFor(status: string): { label: string; action: string; cls: string }[] {
    return this.actionMap[status] || [];
  }

  updateStatus(returnId: number, action: string): void {
    this.actionInProgress = true;
    this.error = '';
    this.successMsg = '';
    this.http.patch<ReturnRequest>(`${this.api}/${returnId}`, { action }).subscribe({
      next: (updated) => {
        const idx = this.returns.findIndex((r) => r.id === returnId);
        if (idx !== -1) this.returns[idx] = updated;
        if (this.selectedReturn?.id === returnId) this.selectedReturn = updated;
        this.applyFilters();
        this.successMsg = `Return #${returnId} updated to "${this.prettify(updated.status)}".`;
        this.actionInProgress = false;
        setTimeout(() => (this.successMsg = ''), 3500);
      },
      error: (err) => {
        this.error = err?.error?.message || `Could not perform action "${action}".`;
        this.actionInProgress = false;
      }
    });
  }

  formatPrice(amount?: number): string {
    return this.currency.format(amount ?? 0);
  }

  formatDate(date?: string | null): string {
    return date ? new Date(date).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—';
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
}
