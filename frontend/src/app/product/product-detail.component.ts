import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService, PendingShopAction } from '../auth/core/auth.service';
import { CartService } from '../cart/cart.service';
import { CatalogMedia, CatalogProductCard, ProductApiService, ProductDetailResponse } from './product-api.service';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  detail?: ProductDetailResponse;
  selectedImage = '';
  loading = true;
  error = '';
  cartNotice = '';
  actionBusy = false;

  private authSubscription?: Subscription;
  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly auth: AuthService,
    private readonly cart: CartService,
    private readonly productApi: ProductApiService
  ) {}

  ngOnInit(): void {
    this.authSubscription = this.auth.authState$.subscribe((loggedIn) => {
      if (loggedIn && this.detail?.product) {
        this.resumePendingAction(this.detail.product);
      }
    });

    this.route.paramMap.subscribe((params) => {
      const slug = params.get('slug');
      if (!slug) {
        this.error = 'Product not found.';
        this.loading = false;
        return;
      }
      this.loadProduct(slug);
    });
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
  }

  get isLoggedIn(): boolean {
    return this.auth.isLoggedIn();
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  primaryImage(product: CatalogProductCard): string {
    return this.selectedImage || product.coverImage || product.media?.[0]?.url || 'assets/logo.svg';
  }

  selectMedia(media: CatalogMedia): void {
    this.selectedImage = media.url;
  }

  discountPercent(product: CatalogProductCard): number {
    if (!product.compareAtCents || product.compareAtCents <= product.priceCents) {
      return 0;
    }
    return Math.round(((product.compareAtCents - product.priceCents) / product.compareAtCents) * 100);
  }

  availabilityText(product: CatalogProductCard): string {
    if (product.stock.inStock) {
      return `${product.stock.stockQty} unit${product.stock.stockQty === 1 ? '' : 's'} ready to ship`;
    }
    return 'Currently unavailable';
  }

  canPurchase(product: CatalogProductCard): boolean {
    return product.stock.inStock;
  }

  addToCart(product: CatalogProductCard): void {
    this.startPurchaseFlow(product, 'add-to-cart');
  }

  buyNow(product: CatalogProductCard): void {
    this.startPurchaseFlow(product, 'buy-now');
  }

  retry(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (slug) {
      this.loadProduct(slug);
    }
  }

  private startPurchaseFlow(product: CatalogProductCard, type: PendingShopAction['type']): void {
    if (!this.canPurchase(product)) {
      return;
    }
    if (!product.sku) {
      this.cartNotice = 'This product is missing a purchasable variant right now.';
      return;
    }
    if (!this.auth.isLoggedIn()) {
      this.promptLoginForAction(product, type);
      return;
    }
    this.performCartAction(product, type);
  }

  private performCartAction(product: CatalogProductCard, type: PendingShopAction['type']): void {
    if (!product.sku || this.actionBusy) {
      return;
    }

    this.actionBusy = true;
    this.cartNotice = '';
    this.cart.addItem(product.sku, 1).subscribe({
      next: () => {
        this.actionBusy = false;
        this.cartNotice = `${product.title} added to cart.`;
        if (type === 'buy-now') {
          this.router.navigate(['/order/checkout']);
        }
      },
      error: (err) => {
        this.actionBusy = false;
        if (err?.status === 401 || err?.status === 403) {
          this.promptLoginForAction(product, type);
          return;
        }
        this.cartNotice = err?.error?.message || 'Could not update the cart right now.';
      }
    });
  }

  private loadProduct(slug: string): void {
    this.loading = true;
    this.error = '';
    this.cartNotice = '';

    this.productApi.getProduct(slug).subscribe({
      next: (detail) => {
        this.detail = detail;
        this.selectedImage = detail.product.coverImage || detail.product.media?.[0]?.url || '';
        this.loading = false;
        this.resumePendingAction(detail.product);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load the product details.';
        this.loading = false;
      }
    });
  }

  private resumePendingAction(product: CatalogProductCard): void {
    if (!this.auth.isLoggedIn()) {
      return;
    }

    const pending = this.auth.consumePendingShopAction();
    if (!pending) {
      return;
    }
    if (pending.slug !== product.slug || pending.sku !== product.sku) {
      this.auth.setPendingShopAction(pending);
      return;
    }
    this.performCartAction(product, pending.type);
  }

  private promptLoginForAction(product: CatalogProductCard, type: PendingShopAction['type']): void {
    if (!product.sku) {
      this.cartNotice = 'This product is missing a purchasable variant right now.';
      return;
    }

    this.auth.logout();
    this.auth.rememberReturnUrl(this.router.url);
    this.auth.setPendingShopAction({ type, sku: product.sku, slug: product.slug });
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { auth: 'login' },
      queryParamsHandling: 'merge'
    });
  }
}