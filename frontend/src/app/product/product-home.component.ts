import { Component, OnInit } from '@angular/core';
import { CatalogHomeResponse, CatalogProductCard, ProductApiService } from './product-api.service';

@Component({
  selector: 'app-product-home',
  templateUrl: './product-home.component.html',
  styleUrls: ['./product-home.component.scss']
})
export class ProductHomeComponent implements OnInit {
  home?: CatalogHomeResponse;
  loading = true;
  error = '';

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(private readonly productApi: ProductApiService) {}

  ngOnInit(): void {
    this.loadHome();
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  primaryImage(product: CatalogProductCard): string {
    return product.coverImage || product.media?.[0]?.url || 'assets/logo.svg';
  }

  discountPercent(product: CatalogProductCard): number {
    if (!product.compareAtCents || product.compareAtCents <= product.priceCents) {
      return 0;
    }
    return Math.round(((product.compareAtCents - product.priceCents) / product.compareAtCents) * 100);
  }

  trackBySlug(_: number, product: CatalogProductCard): string {
    return product.slug;
  }

  retry(): void {
    this.loadHome();
  }

  private loadHome(): void {
    this.loading = true;
    this.error = '';

    this.productApi.getHome().subscribe({
      next: (home) => {
        this.home = home;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load the storefront right now.';
        this.loading = false;
      }
    });
  }
}
