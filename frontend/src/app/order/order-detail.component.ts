import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderDetail, OrderService, RefundSummary, ReturnSummary } from './order.service';

@Component({
  selector: 'app-order-detail',
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss']
})
export class OrderDetailComponent implements OnInit {
  order?: OrderDetail;
  loading = true;
  error = '';
  notice = '';
  cancelling = false;
  returning = false;

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const orderNumber = params.get('orderNumber');
      if (!orderNumber) {
        this.error = 'Order not found.';
        this.loading = false;
        return;
      }
      this.loadOrder(orderNumber);
    });
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '';
  }

  prettify(value?: string): string {
    return (value || 'Unknown')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  toneFor(value?: string): 'success' | 'warning' | 'danger' | 'neutral' {
    const normalized = this.normalize(value);
    if (['success', 'captured', 'authorized', 'paid', 'delivered', 'completed', 'processed', 'refunded'].includes(normalized)) {
      return 'success';
    }
    if (['failed', 'declined', 'cancelled', 'rejected'].includes(normalized)) {
      return 'danger';
    }
    if (['pending', 'initiated', 'processing', 'created', 'packed', 'in_transit', 'refund_pending', 'requested', 'approved', 'received'].includes(normalized)) {
      return 'warning';
    }
    return 'neutral';
  }

  canCancel(order?: OrderDetail): boolean {
    const status = this.normalize(order?.summary.status);
    return !!order && !this.cancelling && !['shipped', 'delivered', 'cancelled', 'failed'].includes(status);
  }

  canReturn(order?: OrderDetail): boolean {
    const status = this.normalize(order?.summary.status);
    return !!order && !this.returning && status === 'delivered' && !order.returnRequest;
  }

  cancelAvailabilityNote(order?: OrderDetail): string {
    if (!order) {
      return '';
    }
    const status = this.normalize(order.summary.status);
    if (this.canCancel(order)) {
      return 'Available until the order moves into shipment.';
    }
    if (['shipped', 'delivered'].includes(status)) {
      return 'This order can no longer be cancelled after shipment.';
    }
    if (status === 'cancelled' && order.refund) {
      return `Refund ${this.prettify(order.refund.status)} for ${this.formatPrice(order.refund.amountCents)}.`;
    }
    if (status === 'cancelled') {
      return 'This order was cancelled.';
    }
    if (status === 'failed') {
      return 'This order is no longer active.';
    }
    return '';
  }

  returnAvailabilityNote(order?: OrderDetail): string {
    if (!order) {
      return '';
    }
    if (this.canReturn(order)) {
      return 'Available after delivery if you need to send the item back.';
    }
    if (order.returnRequest) {
      return `Return ${this.prettify(order.returnRequest.status)}.`;
    }
    if (this.normalize(order.summary.status) !== 'delivered') {
      return 'Returns open only after delivery is completed.';
    }
    return '';
  }

  refundHeadline(refund?: RefundSummary | null): string {
    const status = this.normalize(refund?.status);
    if (status === 'processed') {
      return 'Refund completed';
    }
    if (status === 'failed') {
      return 'Refund needs support';
    }
    return 'Refund in progress';
  }

  refundNote(refund?: RefundSummary | null): string {
    if (!refund) {
      return '';
    }
    const status = this.normalize(refund.status);
    if (status === 'processed') {
      return 'The amount has been sent back to the original payment method.';
    }
    if (status === 'failed') {
      return refund.note || 'The refund could not complete automatically.';
    }
    return refund.note || 'The payment provider is processing the refund now.';
  }

  returnNote(returnRequest?: ReturnSummary | null): string {
    if (!returnRequest) {
      return '';
    }
    const status = this.normalize(returnRequest.status);
    if (status === 'requested') {
      return 'We have recorded the return request and it is waiting for review.';
    }
    if (status === 'approved') {
      return 'The return is approved and moving through the next step.';
    }
    if (status === 'received') {
      return 'The returned item has been received and is being checked.';
    }
    if (status === 'refunded') {
      return 'The return was completed and refund handling has started or finished.';
    }
    if (status === 'rejected') {
      return 'The return request could not be approved.';
    }
    return '';
  }

  canCompletePayment(order?: OrderDetail): boolean {
    if (!order?.payment?.requiresAction) {
      return false;
    }
    const status = this.normalize(order.summary.status);
    const paymentStatus = this.normalize(order.summary.paymentStatus);
    return !['paid', 'shipped', 'delivered', 'cancelled', 'failed'].includes(status)
      && !['success', 'cancelled', 'failed', 'refunded', 'refund_pending'].includes(paymentStatus);
  }

  cancelOrder(): void {
    if (!this.order || !this.canCancel(this.order)) {
      return;
    }
    if (!window.confirm('Cancel this order?')) {
      return;
    }

    this.cancelling = true;
    this.error = '';
    this.notice = '';
    this.orderService.cancelOrder(this.order.summary.orderNumber).subscribe({
      next: (response) => {
        this.cancelling = false;
        if (response.removed) {
          this.router.navigate(['/cart']);
          return;
        }
        if (response.order) {
          this.order = response.order;
        }
        const refundStatus = this.order?.refund ? this.prettify(this.order.refund.status) : null;
        this.notice = refundStatus ? `Order cancelled. Refund ${refundStatus.toLowerCase()} is now tracked below.` : 'Order cancelled successfully.';
      },
      error: (err) => {
        this.cancelling = false;
        this.error = err?.error?.message || 'Could not cancel the order right now.';
      }
    });
  }

  requestReturn(): void {
    if (!this.order || !this.canReturn(this.order)) {
      return;
    }
    if (!window.confirm('Request a return for this delivered order?')) {
      return;
    }

    this.returning = true;
    this.error = '';
    this.notice = '';
    this.orderService.requestReturn(this.order.summary.orderNumber).subscribe({
      next: (order) => {
        this.order = order;
        this.returning = false;
        this.notice = 'Return request created. The progress is now visible in the order detail.';
      },
      error: (err) => {
        this.returning = false;
        this.error = err?.error?.message || 'Could not create the return request right now.';
      }
    });
  }

  private loadOrder(orderNumber: string): void {
    this.loading = true;
    this.error = '';
    this.notice = '';
    this.orderService.getOrder(orderNumber).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load the order.';
        this.loading = false;
      }
    });
  }

  private normalize(value?: string): string {
    return (value || '').trim().toLowerCase();
  }
}