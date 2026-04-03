import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderDetail, OrderService } from './order.service';

interface StatusStep {
  title: string;
  detail: string;
}

@Component({
  selector: 'app-order-status',
  templateUrl: './order-status.component.html',
  styleUrls: ['./order-status.component.scss']
})
export class OrderStatusComponent implements OnInit {
  order?: OrderDetail;
  mode: 'success' | 'failure' = 'success';
  loading = true;
  error = '';

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(private readonly route: ActivatedRoute, private readonly orderService: OrderService) {}

  ngOnInit(): void {
    this.mode = (this.route.snapshot.data['mode'] || 'success') as 'success' | 'failure';
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

  get headline(): string {
    return this.mode === 'success'
      ? 'Payment confirmed. Your order is officially in motion.'
      : 'The order is saved, but payment still needs your attention.';
  }

  get supportCopy(): string {
    return this.mode === 'success'
      ? 'We verified the payment, recorded the order, and moved it into the live tracking flow.'
      : 'Nothing is lost. Review the order, retry the payment, or switch providers from the payment step.';
  }

  get steps(): StatusStep[] {
    if (this.mode === 'success') {
      return [
        { title: 'Order recorded', detail: 'Your items, totals, and addresses are locked into this order.' },
        { title: 'Payment verified', detail: 'The payment callback was confirmed before the order was marked paid.' },
        { title: 'Tracking available', detail: 'You can now open the order detail page and follow every next status update.' }
      ];
    }

    return [
      { title: 'Order saved', detail: 'The order is still recorded, so you do not need to rebuild the cart.' },
      { title: 'Payment pending', detail: 'Use the payment page to retry and let NexBuy confirm the new result.' },
      { title: 'Support ready', detail: 'If the provider keeps failing, review the order and switch to another path.' }
    ];
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '';
  }

  private loadOrder(orderNumber: string): void {
    this.loading = true;
    this.error = '';
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
}