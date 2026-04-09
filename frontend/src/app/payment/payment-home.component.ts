import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderDetail } from '../order/order.service';
import { CompletePaymentRequest, PaymentService } from './payment.service';

declare global {
  interface Window {
    Razorpay?: new (options: any) => {
      open: () => void;
      on?: (eventName: string, handler: (response: any) => void) => void;
    };
  }
}

@Component({
  selector: 'app-payment-home',
  templateUrl: './payment-home.component.html',
  styleUrls: ['./payment-home.component.scss']
})
export class PaymentHomeComponent implements OnInit {
  order?: OrderDetail;
  loading = true;
  error = '';
  processing = false;

  private razorpayScriptPromise?: Promise<void>;
  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const orderNumber = params.get('orderNumber');
      if (!orderNumber) {
        this.error = 'Payment could not be opened.';
        this.loading = false;
        return;
      }
      this.loadPayment(orderNumber);
    });
  }

  get isRazorpayProvider(): boolean {
    return this.order?.payment?.provider === 'razorpay';
  }

  get providerLabel(): string {
    switch (this.order?.payment?.provider) {
      case 'razorpay':
        return 'Razorpay';
      case 'cod':
        return 'Cash on delivery';
      case 'stripe':
        return 'Card payment';
      default:
        return 'Payment';
    }
  }

  get providerMonogram(): string {
    switch (this.order?.payment?.provider) {
      case 'razorpay':
        return 'R';
      case 'cod':
        return 'C';
      case 'stripe':
        return 'S';
      default:
        return 'P';
    }
  }

  get pageHeadline(): string {
    return this.isRazorpayProvider ? 'One quick step left to finish your order.' : 'Complete the payment to finish your order.';
  }

  get supportCopy(): string {
    return this.isRazorpayProvider
      ? ''
      : 'Review the order total and confirm the payment to move this order forward.';
  }

  get primaryActionLabel(): string {
    if (this.processing) {
      return this.isRazorpayProvider ? 'Opening payment...' : 'Processing payment...';
    }
    return this.isRazorpayProvider ? 'Pay now' : 'Confirm payment';
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  simulate(outcome: 'success' | 'failed'): void {
    this.complete({ outcome });
  }

  cancelPayment(): void {
    if (!this.order || this.processing) {
      return;
    }

    this.processing = true;
    this.error = '';
    this.paymentService.cancelPayment(this.order.summary.orderNumber).subscribe({
      next: () => {
        this.processing = false;
        this.router.navigate(['/cart']);
      },
      error: (err) => {
        this.processing = false;
        this.error = err?.error?.message || 'Could not cancel this payment right now.';
      }
    });
  }

  launchRazorpayCheckout(): void {
    const currentOrder = this.order;
    const payment = currentOrder?.payment;
    if (!currentOrder || !payment) {
      return;
    }
    if (!payment.publicKey || !payment.providerOrderId) {
      this.error = 'Razorpay is not fully configured for this order yet.';
      return;
    }

    this.processing = true;
    this.error = '';
    this.ensureRazorpayScript()
      .then(() => {
        if (!window.Razorpay) {
          throw new Error('Razorpay checkout did not load.');
        }

        let checkoutHandled = false;
        const instance = new window.Razorpay({
          key: payment.publicKey,
          amount: currentOrder.summary.totalCents,
          currency: currentOrder.summary.currency || 'INR',
          name: 'NexBuy',
          description: `Order ${currentOrder.summary.orderNumber}`,
          order_id: payment.providerOrderId,
          handler: (response: any) => {
            checkoutHandled = true;
            this.complete({
              outcome: 'success',
              providerOrderId: response?.razorpay_order_id,
              providerPaymentId: response?.razorpay_payment_id,
              signature: response?.razorpay_signature
            });
          },
          modal: {
            ondismiss: () => {
              if (checkoutHandled) {
                return;
              }
              this.processing = false;
              this.error = 'Payment window was closed before the payment was completed.';
            }
          },
          theme: {
            color: '#df6d2b'
          }
        });

        instance.on?.('payment.failed', (response: any) => {
          checkoutHandled = true;
          this.complete({
            outcome: 'failed',
            providerOrderId: response?.error?.metadata?.order_id || payment.providerOrderId,
            providerPaymentId: response?.error?.metadata?.payment_id
          });
        });

        instance.open();
      })
      .catch((err) => {
        this.processing = false;
        this.error = err?.message || 'Could not open Razorpay right now.';
      });
  }

  private complete(body: CompletePaymentRequest): void {
    if (!this.order) {
      return;
    }

    this.processing = true;
    this.error = '';
    this.paymentService.completePayment(this.order.summary.orderNumber, body).subscribe({
      next: (response) => {
        this.processing = false;
        const target = body.outcome === 'success' ? ['/order/success', response.order.summary.orderNumber] : ['/order/failure', response.order.summary.orderNumber];
        this.router.navigate(target);
      },
      error: (err) => {
        this.processing = false;
        this.error = err?.error?.message || 'Could not update the payment.';
      }
    });
  }

  private loadPayment(orderNumber: string): void {
    this.loading = true;
    this.error = '';
    this.paymentService.getPayment(orderNumber).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
        if (order.summary.paymentStatus === 'success') {
          this.router.navigate(['/order/success', order.summary.orderNumber]);
        }
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load the payment step.';
        this.loading = false;
      }
    });
  }

  private ensureRazorpayScript(): Promise<void> {
    if (window.Razorpay) {
      return Promise.resolve();
    }
    if (this.razorpayScriptPromise) {
      return this.razorpayScriptPromise;
    }

    this.razorpayScriptPromise = new Promise<void>((resolve, reject) => {
      const existing = document.querySelector('script[data-razorpay-checkout="true"]') as HTMLScriptElement | null;
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error('Razorpay checkout script failed to load.')), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.dataset['razorpayCheckout'] = 'true';
      script.setAttribute('integrity', 'sha384-...');
      script.setAttribute('crossorigin', 'anonymous');
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Razorpay checkout script failed to load.'));
      document.body.appendChild(script);
    });

    return this.razorpayScriptPromise;
  }
}
