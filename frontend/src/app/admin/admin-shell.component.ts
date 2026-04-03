import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter, startWith } from 'rxjs';
import { AuthService } from '../auth/core/auth.service';

@Component({
  selector: 'app-admin-shell',
  templateUrl: './admin-shell.component.html',
  styleUrls: ['./admin-shell.component.scss']
})
export class AdminShellComponent implements OnInit {
  pageTitle = 'Dashboard';
  sidebarOpen = false;
  navItems = [
    { label: 'Dashboard', icon: 'dashboard', path: '/admin/dashboard' },
    { label: 'Forecasting', icon: 'insights', path: '/admin/forecast' },
    { label: 'View Users', icon: 'groups', path: '/admin/users' },
    { label: 'Manage Orders', icon: 'receipt_long', path: '/admin/orders' },
    { label: 'Manage Products', icon: 'inventory_2', path: '/admin/products' },
    { label: 'Add Product', icon: 'add_box', path: '/admin/products/new' },
    { label: 'Add Admin', icon: 'shield_person', path: '/admin/admins' }
  ];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        startWith(new NavigationEnd(0, this.router.url, this.router.url))
      )
      .subscribe(() => {
        this.pageTitle = this.resolveTitle(this.route);
        this.sidebarOpen = false;
      });
  }

  get adminEmail(): string {
    return localStorage.getItem('userEmail') || 'admin@nexbuy.com';
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/']);
  }

  private resolveTitle(route: ActivatedRoute): string {
    let current: ActivatedRoute | null = route.firstChild;
    while (current?.firstChild) {
      current = current.firstChild;
    }
    return current?.snapshot.data['title'] || 'Dashboard';
  }
}