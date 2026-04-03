import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AdminApiService, AdminProduct } from './admin-api.service';

@Component({
  selector: 'app-admin-products',
  templateUrl: './admin-products.component.html',
  styleUrls: ['./admin-products.component.scss']
})
export class AdminProductsComponent implements OnInit {
  products: AdminProduct[] = [];
  loading = true;
  query = '';
  deletingId: number | null = null;
  stockSavingId: number | null = null;
  quickStockInputs: Record<number, number | null> = {};

  constructor(private adminApi: AdminApiService, private router: Router) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  get filteredProducts(): AdminProduct[] {
    const term = this.query.trim().toLowerCase();
    if (!term) {
      return this.products;
    }
    return this.products.filter((product) =>
      [
        product.title,
        product.categoryName || '',
        product.brandName || '',
        product.sku || '',
        product.status,
        (product.tags || []).join(' ')
      ].some((value) => value.toLowerCase().includes(term))
    );
  }

  editProduct(productId: number): void {
    this.router.navigate(['/admin/products', productId, 'edit']);
  }

  deleteProduct(product: AdminProduct): void {
    const confirmed = confirm(`Delete ${product.title}?`);
    if (!confirmed) {
      return;
    }
    this.deletingId = product.id;
    this.adminApi.deleteProduct(product.id).subscribe({
      next: () => {
        this.products = this.products.filter((item) => item.id !== product.id);
        this.deletingId = null;
      },
      error: () => {
        this.deletingId = null;
      }
    });
  }

  formatPrice(priceCents: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(priceCents / 100);
  }

  previewImage(product: AdminProduct): string {
    return product.coverImage || product.media?.[0]?.url || 'assets/logo.svg';
  }

  discountPercent(product: AdminProduct): number {
    const compareAt = product.compareAtCents ?? 0;
    if (!compareAt || compareAt <= product.priceCents) {
      return 0;
    }
    return Math.round(((compareAt - product.priceCents) / compareAt) * 100);
  }

  quickStockValue(productId: number): number | null {
    return this.quickStockInputs[productId] ?? null;
  }

  applyQuickStock(product: AdminProduct, delta: number): void {
    if (this.stockSavingId) {
      return;
    }
    this.stockSavingId = product.id;
    this.adminApi.updateProductStock(product.id, { stockDelta: delta }).subscribe({
      next: (updated) => {
        this.replaceProduct(updated);
        this.stockSavingId = null;
      },
      error: () => {
        this.stockSavingId = null;
      }
    });
  }

  setQuickStock(product: AdminProduct): void {
    const nextStock = this.quickStockValue(product.id);
    if (nextStock === null || nextStock < 0 || this.stockSavingId) {
      return;
    }
    this.stockSavingId = product.id;
    this.adminApi.updateProductStock(product.id, { stockQty: nextStock }).subscribe({
      next: (updated) => {
        this.replaceProduct(updated);
        this.quickStockInputs[product.id] = null;
        this.stockSavingId = null;
      },
      error: () => {
        this.stockSavingId = null;
      }
    });
  }

  private loadProducts(): void {
    this.loading = true;
    this.adminApi.getProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private replaceProduct(updated: AdminProduct): void {
    this.products = this.products.map((product) => product.id === updated.id ? updated : product);
  }
}
