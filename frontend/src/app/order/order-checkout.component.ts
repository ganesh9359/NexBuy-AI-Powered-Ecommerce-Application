import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartService } from '../cart/cart.service';
import { AddressSummary, CheckoutViewResponse, OrderService, UpsertAddressRequest } from './order.service';

@Component({
  selector: 'app-order-checkout',
  templateUrl: './order-checkout.component.html',
  styleUrls: ['./order-checkout.component.scss']
})
export class OrderCheckoutComponent implements OnInit {
  checkout?: CheckoutViewResponse;
  loading = true;
  error = '';
  submitting = false;
  savingAddress = false;
  showAddressForm = false;
  selectedShippingId?: number;
  selectedBillingId?: number;
  billingSameAsShipping = true;
  selectedProvider = 'cod';
  addressForm: UpsertAddressRequest = {
    label: 'Home',
    line1: '',
    line2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'India',
    isDefault: false
  };

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(
    private readonly orderService: OrderService,
    private readonly cartService: CartService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadCheckout();
  }

  get hasStockIssues(): boolean {
    return !!this.checkout?.cart.items.some((item) => !item.purchasable);
  }

  get stockIssueSummary(): string {
    const issue = this.checkout?.cart.items.find((item) => !item.purchasable);
    if (!issue) {
      return '';
    }
    if (issue.stockQty > 0) {
      return `${issue.title} has only ${issue.stockQty} unit${issue.stockQty === 1 ? '' : 's'} available. Go back to cart and reduce the quantity.`;
    }
    return `${issue.title} is out of stock. Go back to cart before placing the order.`;
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  trackByAddress(_: number, address: AddressSummary): number {
    return address.id;
  }

  providerTitle(provider?: string | null): string {
    switch ((provider || '').toLowerCase()) {
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

  providerDescription(provider?: string | null): string {
    switch ((provider || '').toLowerCase()) {
      case 'razorpay':
        return 'Pay securely in Razorpay test checkout and verify the payment on the server.';
      case 'cod':
        return 'Place the order now and pay when the shipment arrives at your address.';
      default:
        return 'Choose how you would like to complete the order.';
    }
  }

  providerSupportCopy(provider?: string | null): string {
    switch ((provider || '').toLowerCase()) {
      case 'razorpay':
        return 'Recommended for instant confirmation';
      case 'cod':
        return 'Best if you prefer payment on delivery';
      default:
        return 'Provider available';
    }
  }

  placeOrderLabel(): string {
    if (this.hasStockIssues) {
      return 'Update cart stock to continue';
    }
    switch (this.selectedProvider) {
      case 'cod':
        return 'Place order with cash on delivery';
      case 'razorpay':
        return 'Continue to Razorpay';
      default:
        return 'Place order';
    }
  }

  saveAddress(): void {
    if (!this.addressForm.line1 || !this.addressForm.city || !this.addressForm.state || !this.addressForm.postalCode || !this.addressForm.country) {
      this.error = 'Please complete the address form before saving.';
      return;
    }

    this.savingAddress = true;
    this.error = '';
    this.orderService.createAddress(this.addressForm).subscribe({
      next: (address) => {
        this.savingAddress = false;
        if (!this.checkout) {
          return;
        }
        this.checkout = {
          ...this.checkout,
          addresses: [address, ...this.checkout.addresses.filter((item) => item.id !== address.id)]
        };
        this.selectedShippingId = address.id;
        this.selectedBillingId = address.id;
        this.billingSameAsShipping = true;
        this.showAddressForm = false;
        this.addressForm = {
          label: 'Home',
          line1: '',
          line2: '',
          city: '',
          state: '',
          postalCode: '',
          country: 'India',
          isDefault: false
        };
      },
      error: (err) => {
        this.savingAddress = false;
        this.error = err?.error?.message || 'Could not save the address right now.';
      }
    });
  }

  placeOrder(): void {
    if (this.hasStockIssues) {
      this.error = this.stockIssueSummary;
      return;
    }
    if (!this.selectedShippingId) {
      this.error = 'Please choose a shipping address.';
      return;
    }

    this.submitting = true;
    this.error = '';
    this.orderService.placeOrder({
      shippingAddressId: this.selectedShippingId,
      billingAddressId: this.billingSameAsShipping ? this.selectedShippingId : this.selectedBillingId || null,
      billingSameAsShipping: this.billingSameAsShipping,
      paymentProvider: this.selectedProvider
    }).subscribe({
      next: (response) => {
        this.submitting = false;
        this.cartService.loadCart().subscribe();
        if (response.order.summary.paymentStatus === 'success') {
          this.router.navigate(['/order/success', response.order.summary.orderNumber]);
          return;
        }
        this.router.navigate(['/payment', response.order.summary.orderNumber]);
      },
      error: (err) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Could not place the order.';
      }
    });
  }

  private loadCheckout(): void {
    this.loading = true;
    this.error = '';
    this.orderService.getCheckout().subscribe({
      next: (checkout) => {
        const addresses = checkout.addresses ?? [];
        const availableProviders = checkout.availableProviders?.length ? checkout.availableProviders : ['cod'];
        const recommendedProvider = checkout.recommendedProvider || availableProviders[0] || 'cod';

        this.checkout = {
          ...checkout,
          addresses,
          availableProviders,
          recommendedProvider
        };
        this.selectedProvider = recommendedProvider;
        const preferredAddress = addresses.find((address) => address.isDefault) || addresses[0];
        this.selectedShippingId = preferredAddress?.id;
        this.selectedBillingId = preferredAddress?.id;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not prepare checkout.';
        this.loading = false;
      }
    });
  }
}
