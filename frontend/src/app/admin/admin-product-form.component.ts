import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  AdminApiService,
  AdminBrand,
  AdminProductMedia,
  AdminProductPayload
} from './admin-api.service';
import { STORE_CATEGORIES } from '../shared/store-categories';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-admin-product-form',
  templateUrl: './admin-product-form.component.html',
  styleUrls: ['./admin-product-form.component.scss']
})
export class AdminProductFormComponent implements OnInit {
  productId?: number;
  saving = false;
  loading = false;
  uploadingImage = false;
  uploadingGallery = false;
  creatingBrand = false;
  error = '';
  imageNotice = '';
  newBrandName = '';
  tagsText = '';
  brands: AdminBrand[] = [];
  readonly statuses = ['active', 'inactive'];
  readonly storefrontCategories = STORE_CATEGORIES.map((category) => category.name);
  form: AdminProductPayload = {
    title: '',
    slug: '',
    description: '',
    coverImage: '',
    categoryName: '',
    brandName: '',
    sku: '',
    variantName: '',
    status: 'active',
    tags: [],
    media: [],
    priceCents: 0,
    stockQty: 0
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private adminApi: AdminApiService
  ) {}

  ngOnInit(): void {
    this.loadBrands();

    const rawId = this.route.snapshot.paramMap.get('id');
    if (rawId) {
      this.productId = Number(rawId);
      this.loadProduct(this.productId);
    }
  }

  get isEditMode(): boolean {
    return !!this.productId;
  }

  get categoryOptions(): string[] {
    const options = [...this.storefrontCategories];
    const current = this.form.categoryName?.trim();
    if (current && !options.includes(current)) {
      options.push(current);
    }
    return options;
  }

  get brandOptions(): string[] {
    const options = this.brands.map((brand) => brand.name);
    const current = this.form.brandName?.trim();
    if (current && !options.includes(current)) {
      options.push(current);
    }
    return options.sort((a, b) => a.localeCompare(b));
  }

  get previewImage(): string {
    const image = this.form.coverImage?.trim() || this.form.media?.[0]?.url?.trim();
    if (!image) {
      return 'assets/logo.svg';
    }
    return this.resolveImage(image);
  }

  get galleryItems(): AdminProductMedia[] {
    return this.form.media || [];
  }

  get isUploading(): boolean {
    return this.uploadingImage || this.uploadingGallery || this.creatingBrand;
  }

  onCoverImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.error = 'Please choose a valid image file';
      input.value = '';
      return;
    }

    this.uploadingImage = true;
    this.error = '';
    this.imageNotice = '';

    this.adminApi.uploadProductImage(file).subscribe({
      next: (res) => {
        this.form.coverImage = res.url;
        this.appendMedia(res.url, this.form.title || 'Product image');
        this.imageNotice = 'Cover image uploaded successfully.';
        this.uploadingImage = false;
        input.value = '';
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not upload image';
        this.uploadingImage = false;
        input.value = '';
      }
    });
  }

  onGalleryImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    if (!files.length) {
      return;
    }

    const invalid = files.find((file) => !file.type.startsWith('image/'));
    if (invalid) {
      this.error = 'Please choose image files only';
      input.value = '';
      return;
    }

    this.uploadingGallery = true;
    this.error = '';
    this.imageNotice = '';

    forkJoin(files.map((file) => this.adminApi.uploadProductImage(file))).subscribe({
      next: (uploads) => {
        uploads.forEach((upload) => this.appendMedia(upload.url));
        if (!this.form.coverImage?.trim() && uploads[0]?.url) {
          this.form.coverImage = uploads[0].url;
        }
        this.imageNotice = `${uploads.length} gallery image${uploads.length > 1 ? 's' : ''} uploaded successfully.`;
        this.uploadingGallery = false;
        input.value = '';
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not upload gallery images';
        this.uploadingGallery = false;
        input.value = '';
      }
    });
  }

  addMediaRow(): void {
    this.form.media = [...this.galleryItems, { url: '', altText: '', sortOrder: this.galleryItems.length }];
  }

  removeMedia(index: number): void {
    this.form.media = this.normalizeMediaEntries(
      this.galleryItems.filter((_, itemIndex) => itemIndex !== index),
      this.form.coverImage
    );
    if (this.form.coverImage && !this.form.media.some((item) => item.url === this.form.coverImage)) {
      this.form.coverImage = this.form.media?.[0]?.url || '';
    }
  }

  moveMedia(index: number, direction: number): void {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= this.galleryItems.length) {
      return;
    }

    const next = [...this.galleryItems];
    const [item] = next.splice(index, 1);
    next.splice(targetIndex, 0, item);
    this.form.media = this.normalizeMediaEntries(next, this.form.coverImage);
  }

  setCoverFromMedia(url: string): void {
    this.form.coverImage = url;
    this.form.media = this.normalizeMediaEntries(this.galleryItems, this.form.coverImage);
  }

  createBrand(): void {
    const name = this.newBrandName.trim();
    if (!name) {
      this.error = 'Brand name is required';
      return;
    }

    this.creatingBrand = true;
    this.error = '';

    this.adminApi.createBrand({ name }).subscribe({
      next: (brand) => {
        this.brands = this.sortBrands([...this.brands, brand]);
        this.form.brandName = brand.name;
        this.newBrandName = '';
        this.creatingBrand = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not create brand';
        this.creatingBrand = false;
      }
    });
  }

  save(): void {
    if (!this.form.title.trim()) {
      this.error = 'Product title is required';
      return;
    }
    if (!this.form.categoryName?.trim()) {
      this.error = 'Please choose a category';
      return;
    }

    this.saving = true;
    this.error = '';

    const payload: AdminProductPayload = {
      title: this.form.title.trim(),
      slug: this.trimOrUndefined(this.form.slug),
      description: this.trimOrUndefined(this.form.description),
      coverImage: this.trimOrUndefined(this.form.coverImage),
      categoryName: this.trimOrUndefined(this.form.categoryName),
      brandName: this.trimOrUndefined(this.form.brandName),
      sku: this.trimOrUndefined(this.form.sku),
      variantName: this.trimOrUndefined(this.form.variantName),
      status: this.form.status,
      tags: this.parseTags(this.tagsText),
      media: this.normalizeMediaEntries(this.galleryItems, this.form.coverImage),
      priceCents: this.form.priceCents,
      stockQty: this.form.stockQty
    };

    const request = this.isEditMode
      ? this.adminApi.updateProduct(this.productId!, payload)
      : this.adminApi.createProduct(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.router.navigate(['/admin/products']);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not save product';
        this.saving = false;
      }
    });
  }

  resolveImage(url: string): string {
    if (!url) {
      return 'assets/logo.svg';
    }
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('assets/')) {
      return url;
    }
    return `${environment.apiBase}${url.startsWith('/') ? '' : '/'}${url}`;
  }

  private loadBrands(): void {
    this.adminApi.getBrands().subscribe({
      next: (brands) => {
        this.brands = this.sortBrands(brands);
      }
    });
  }

  private loadProduct(productId: number): void {
    this.loading = true;
    this.adminApi.getProduct(productId).subscribe({
      next: (product) => {
        this.form = {
          title: product.title,
          slug: product.slug,
          description: product.description || '',
          coverImage: product.coverImage || '',
          categoryName: product.categoryName || '',
          brandName: product.brandName || '',
          sku: product.sku || '',
          variantName: product.variantName || product.title,
          status: product.status.toLowerCase(),
          tags: product.tags || [],
          media: this.normalizeMediaEntries(
            product.media?.length
              ? product.media
              : product.coverImage
                ? [{ url: product.coverImage, altText: `${product.title} cover image`, sortOrder: 0 }]
                : [],
            product.coverImage || ''
          ),
          priceCents: product.priceCents,
          stockQty: product.stockQty
        };
        this.tagsText = (product.tags || []).join(', ');
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load product';
        this.loading = false;
      }
    });
  }

  private appendMedia(url: string, altText = ''): void {
    this.form.media = this.normalizeMediaEntries(
      [...this.galleryItems, { url, altText, sortOrder: this.galleryItems.length }],
      this.form.coverImage
    );
  }

  private parseTags(value: string): string[] {
    const unique = new Set<string>();
    value
      .split(',')
      .map((tag) => tag.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, ''))
      .filter(Boolean)
      .forEach((tag) => unique.add(tag));
    return Array.from(unique);
  }

  private normalizeMediaEntries(media: AdminProductMedia[], preferredCoverImage?: string): AdminProductMedia[] {
    const unique = new Map<string, AdminProductMedia>();

    media.forEach((item) => {
      const url = item.url?.trim();
      if (!url) {
        return;
      }

      if (!unique.has(url)) {
        unique.set(url, {
          ...item,
          url,
          altText: item.altText?.trim() || ''
        });
        return;
      }

      const existing = unique.get(url)!;
      if (!existing.altText && item.altText?.trim()) {
        existing.altText = item.altText.trim();
      }
    });

    const entries = Array.from(unique.values()).map((item, index) => ({
      ...item,
      sortOrder: index
    }));

    const cover = preferredCoverImage?.trim() || this.form.coverImage?.trim();
    if (cover) {
      const coverIndex = entries.findIndex((item) => item.url === cover);
      if (coverIndex > 0) {
        const [coverItem] = entries.splice(coverIndex, 1);
        entries.unshift(coverItem);
      }
    }

    return entries.map((item, index) => ({ ...item, sortOrder: index }));
  }

  private trimOrUndefined(value?: string): string | undefined {
    const trimmed = value?.trim();
    return trimmed ? trimmed : undefined;
  }

  private sortBrands(brands: AdminBrand[]): AdminBrand[] {
    return [...brands].sort((left, right) => left.name.localeCompare(right.name));
  }
}
