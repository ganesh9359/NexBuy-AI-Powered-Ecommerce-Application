import { Component, OnInit } from '@angular/core';
import { AdminApiService, AdminForecast, AdminForecastPoint, AdminForecastRisk } from './admin-api.service';

interface ForecastProjectionPoint {
  label: string;
  value: number;
  phase: 'actual' | 'forecast';
}

@Component({
  selector: 'app-admin-forecast',
  templateUrl: './admin-forecast.component.html',
  styleUrls: ['./admin-forecast.component.scss']
})
export class AdminForecastComponent implements OnInit {
  forecast?: AdminForecast;
  loading = true;
  error = '';

  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(private readonly adminApi: AdminApiService) {}

  ngOnInit(): void {
    this.loadForecast();
  }

  get revenueProjection(): ForecastProjectionPoint[] {
    return this.buildProjection(this.forecast?.revenueTrend || [], this.forecast?.overview.projectedRevenueCents || 0);
  }

  get orderProjection(): ForecastProjectionPoint[] {
    return this.buildProjection(this.forecast?.orderTrend || [], this.forecast?.overview.projectedOrders || 0);
  }

  get revenueMax(): number {
    return Math.max(1, ...this.revenueProjection.map((point) => point.value || 0));
  }

  get orderMax(): number {
    return Math.max(1, ...this.orderProjection.map((point) => point.value || 0));
  }

  get projectedDailyRevenue(): number {
    return Math.round((this.forecast?.overview.projectedRevenueCents || 0) / 7);
  }

  get projectedDailyOrders(): number {
    return Math.round((this.forecast?.overview.projectedOrders || 0) / 7);
  }

  get forecastWindow(): string {
    const start = new Date();
    start.setDate(start.getDate() + 1);
    const end = new Date(start);
    end.setDate(end.getDate() + 6);
    const formatter = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short' });
    return `${formatter.format(start)} - ${formatter.format(end)}`;
  }

  get confidenceLabel(): string {
    const volatility = this.volatilityScore(this.forecast?.revenueTrend || []);
    if (volatility <= 0.18) {
      return 'High confidence';
    }
    if (volatility <= 0.35) {
      return 'Measured confidence';
    }
    return 'Guarded confidence';
  }

  get confidenceNote(): string {
    const pendingOrders = this.forecast?.overview.pendingOrders || 0;
    const volatility = this.volatilityScore(this.forecast?.revenueTrend || []);
    if (volatility <= 0.18 && pendingOrders <= 5) {
      return 'Recent demand is stable, so the next-week projection should be dependable under normal trading conditions.';
    }
    if (volatility <= 0.35) {
      return 'Demand is moving, but still inside a usable range. Keep an eye on stock risk and pending orders while using this forecast.';
    }
    return 'Demand has been volatile lately. Use this forecast as a planning guide, but hold room for promotion or stock shocks.';
  }

  get topDemandLabel(): string {
    const topCategory = this.forecast?.categoryDemand?.[0];
    return topCategory ? `${topCategory.category} is the strongest projected demand lane.` : 'No demand leader yet.';
  }

  retry(): void {
    this.loadForecast();
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  formatTrend(rate?: number | null): string {
    const value = rate ?? 0;
    const sign = value > 0 ? '+' : '';
    return `${sign}${value.toFixed(1)}%`;
  }

  width(value: number, max: number): string {
    return `${Math.max(10, (value / Math.max(max, 1)) * 100)}%`;
  }

  coverageDays(risk: AdminForecastRisk): number {
    const averageDailyDemand = Math.max((risk.soldLast30Days || 0) / 30, 0.35);
    return Math.max(0, Math.round(risk.stockQty / averageDailyDemand));
  }

  private loadForecast(): void {
    this.loading = true;
    this.error = '';
    this.adminApi.getForecast().subscribe({
      next: (forecast) => {
        this.forecast = forecast;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load the forecast right now.';
        this.loading = false;
      }
    });
  }

  private buildProjection(history: AdminForecastPoint[], projectedTotal: number): ForecastProjectionPoint[] {
    const recent = history.slice(-7).map((point) => ({
      label: point.label,
      value: point.value,
      phase: 'actual' as const
    }));
    const actualValues = recent.map((point) => point.value || 0);
    const baseTotal = actualValues.reduce((sum, value) => sum + value, 0);
    const weights = baseTotal > 0
      ? actualValues.map((value) => value / baseTotal)
      : new Array(Math.max(recent.length, 7)).fill(1 / Math.max(recent.length, 7));

    const start = new Date();
    const formatter = new Intl.DateTimeFormat('en-IN', { weekday: 'short' });
    const forecast = new Array(7).fill(0).map((_, index) => {
      const day = new Date(start);
      day.setDate(start.getDate() + index + 1);
      const baseWeight = weights[index % weights.length] || 1 / 7;
      return {
        label: formatter.format(day),
        value: Math.max(0, Math.round(projectedTotal * baseWeight)),
        phase: 'forecast' as const
      };
    });

    return [...recent, ...forecast];
  }

  private volatilityScore(points: AdminForecastPoint[]): number {
    const values = points.slice(-7).map((point) => point.value || 0).filter((value) => value > 0);
    if (!values.length) {
      return 0;
    }
    const mean = values.reduce((sum, value) => sum + value, 0) / values.length;
    if (mean <= 0) {
      return 0;
    }
    const variance = values.reduce((sum, value) => sum + Math.pow(value - mean, 2), 0) / values.length;
    return Math.sqrt(variance) / mean;
  }
}
