import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartLineItem, CartResponse, CartService } from './cart.service';

@Component({
  selector: 'app-cart-home',
  templateUrl: './cart-home.component.html',
  styleUrls: ['./cart-home.component.scss']
})
export class CartHomeComponent implements OnInit {
  cart?: CartResponse;
  loading = true;
  error = '';
  busyItemId?: number;
  clearing = false;

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(private readonly cartService: CartService, private readonly router: Router) {}

  ngOnInit(): void {
    this.loadCart();
  }

  get items(): CartLineItem[] {
    return this.cart?.items || [];
  }

  get hasStockIssues(): boolean {
    return this.items.some((item) => !item.purchasable);
  }

  get stockIssueSummary(): string {
    const issue = this.items.find((item) => !item.purchasable);
    if (!issue) {
      return '';
    }
    if (issue.stockQty > 0) {
      return `${issue.title} has only ${issue.stockQty} unit${issue.stockQty === 1 ? '' : 's'} available. Reduce the quantity to continue.`;
    }
    return `${issue.title} is out of stock. Remove it or reduce the quantity to continue.`;
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  canIncrease(item: CartLineItem): boolean {
    return item.quantity < item.stockQty;
  }

  increase(item: CartLineItem): void {
    if (!this.canIncrease(item)) {
      this.error = item.stockQty > 0
        ? `Only ${item.stockQty} unit${item.stockQty === 1 ? '' : 's'} available for ${item.title}.`
        : `${item.title} is out of stock.`;
      return;
    }
    this.updateQuantity(item, item.quantity + 1);
  }

  decrease(item: CartLineItem): void {
    this.updateQuantity(item, item.quantity - 1);
  }

  remove(item: CartLineItem): void {
    this.busyItemId = item.itemId;
    this.cartService.removeItem(item.itemId).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.busyItemId = undefined;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not update the cart right now.';
        this.busyItemId = undefined;
      }
    });
  }

  clear(): void {
    this.clearing = true;
    this.cartService.clear().subscribe({
      next: (cart) => {
        this.cart = cart;
        this.clearing = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not clear the cart right now.';
        this.clearing = false;
      }
    });
  }

  goToCheckout(): void {
    if (this.hasStockIssues) {
      this.error = this.stockIssueSummary;
      return;
    }
    this.router.navigate(['/order/checkout']);
  }

  trackByItemId(_: number, item: CartLineItem): number {
    return item.itemId;
  }

  private loadCart(): void {
    this.loading = true;
    this.error = '';
    this.cartService.loadCart().subscribe({
      next: (cart) => {
        this.cart = cart;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load your cart.';
        this.loading = false;
      }
    });
  }

  private updateQuantity(item: CartLineItem, quantity: number): void {
    this.busyItemId = item.itemId;
    this.error = '';
    this.cartService.updateQuantity(item.itemId, quantity).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.busyItemId = undefined;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not update the cart right now.';
        this.busyItemId = undefined;
      }
    });
  }
}