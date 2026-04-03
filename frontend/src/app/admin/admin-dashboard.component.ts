import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AdminApiService, AdminDashboard } from './admin-api.service';

interface StatusLegendItem {
  key: string;
  label: string;
  color: string;
  value: number;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  dashboard?: AdminDashboard;
  loading = true;
  statusLegend: StatusLegendItem[] = [];

  private readonly statusPalette: Record<string, string> = {
    pending: '#38bdf8',
    paid: '#fb923c',
    shipped: '#818cf8',
    delivered: '#22c55e',
    cancelled: '#fb7185',
    failed: '#ef4444'
  };

  constructor(private adminApi: AdminApiService, private router: Router) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  get chartBackground(): string {
    const items = this.statusLegend.filter((item) => item.value > 0);
    if (!items.length) {
      return 'conic-gradient(#cbd5e1 0 100%)';
    }
    const total = items.reduce((sum, item) => sum + item.value, 0);
    let start = 0;
    const segments = items.map((item) => {
      const span = (item.value / total) * 100;
      const end = start + span;
      const segment = `${item.color} ${start}% ${end}%`;
      start = end;
      return segment;
    });
    return `conic-gradient(${segments.join(', ')})`;
  }

  openUsers(): void {
    this.router.navigate(['/admin/users']);
  }

  openProducts(): void {
    this.router.navigate(['/admin/products']);
  }

  openOrders(status?: string): void {
    this.router.navigate(['/admin/orders'], {
      queryParams: status ? { status: status.toUpperCase() } : {}
    });
  }

  openForecast(): void {
    this.router.navigate(['/admin/forecast']);
  }

  openStatus(item: StatusLegendItem): void {
    if (!item.value) {
      return;
    }
    this.openOrders(item.key);
  }

  private loadDashboard(): void {
    this.loading = true;
    this.adminApi.getDashboard().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.statusLegend = Object.entries(this.statusPalette).map(([key, color]) => ({
          key,
          color,
          value: dashboard.orderStatusCounts?.[key] ?? 0,
          label: key.charAt(0).toUpperCase() + key.slice(1)
        }));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}