import { Injectable } from '@angular/core';
import { CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(_: any, state: RouterStateSnapshot): boolean | UrlTree {
    if (this.auth.isLoggedIn()) {
      return true;
    }

    this.auth.rememberReturnUrl(state.url);
    const fallback = this.normalizeFallback(this.auth.getLastVisitedUrl());
    const tree = this.router.parseUrl(fallback);
    tree.queryParams = { ...tree.queryParams, reason: 'unauthorized' };
    return tree;
  }

  private normalizeFallback(url: string | null): string {
    const candidate = url || '/product';
    const path = candidate.split('?')[0];
    if (this.isProtectedPath(path)) {
      return '/product';
    }
    return candidate;
  }

  private isProtectedPath(path: string): boolean {
    return ['/user', '/cart', '/order', '/payment', '/admin'].some((prefix) => path === prefix || path.startsWith(`${prefix}/`));
  }
}