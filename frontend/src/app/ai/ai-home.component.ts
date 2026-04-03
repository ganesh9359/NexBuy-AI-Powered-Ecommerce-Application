import { Component, OnInit } from '@angular/core';
import { AiRecommendationResponse, AiService } from './ai.service';
import { CatalogProductCard } from '../product/product-api.service';

@Component({
  selector: 'app-ai-home',
  templateUrl: './ai-home.component.html',
  styleUrls: ['./ai-home.component.scss']
})
export class AiHomeComponent implements OnInit {
  recommendationsLoading = true;
  recommendationsError = '';
  recommendations?: AiRecommendationResponse;

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(private readonly aiService: AiService) {}

  ngOnInit(): void {
    this.loadRecommendations();
  }

  get recommendationMode(): string {
    return this.recommendations?.personalized ? 'Personalized' : 'Live trend';
  }

  get recommendationCount(): number {
    return this.recommendations?.products?.length || 0;
  }

  openAssistant(): void {
    window.dispatchEvent(new CustomEvent('nexbuy:open-chatbot'));
  }

  loadRecommendations(): void {
    this.recommendationsLoading = true;
    this.recommendationsError = '';
    this.aiService.getRecommendations().subscribe({
      next: (response) => {
        this.recommendations = response;
        this.recommendationsLoading = false;
      },
      error: (err) => {
        this.recommendationsError = err?.error?.message || 'Could not load recommendations right now.';
        this.recommendationsLoading = false;
      }
    });
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  trackBySlug(_index: number, product: CatalogProductCard): string {
    return product.slug;
  }

  trackByText(_index: number, value: string): string {
    return value;
  }
}
