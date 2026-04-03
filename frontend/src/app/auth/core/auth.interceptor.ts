import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse
} from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.auth.token;
    const cloned = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

    return next.handle(cloned).pipe(
      catchError((err: HttpErrorResponse) => {
        if ((err.status === 401 || this.isCommerceAuthFailure(err, token)) && token) {
          this.auth.logout();
          this.auth.rememberReturnUrl(this.router.url);
          this.router.navigateByUrl(this.withLoginPrompt(this.router.url));
        }
        return throwError(() => err);
      })
    );
  }

  private isCommerceAuthFailure(err: HttpErrorResponse, token: string | null): boolean {
    if (err.status !== 403 || !token) {
      return false;
    }
    return ['/cart', '/orders', '/payments', '/users/me'].some((segment) => err.url?.includes(segment));
  }

  private withLoginPrompt(url: string): string {
    const base = url || '/product';
    const [path, query] = base.split('?');
    const params = new URLSearchParams(query || '');
    params.set('auth', 'login');
    const next = params.toString();
    return next ? `${path}?${next}` : `${path}?auth=login`;
  }
}