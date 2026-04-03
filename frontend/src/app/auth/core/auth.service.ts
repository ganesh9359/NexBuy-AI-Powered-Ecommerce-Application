import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthResponse {
  token?: string;
  refreshToken?: string;
  email: string;
  requiresOtp?: boolean;
  userId?: number;
  role?: string;
}

export interface PendingShopAction {
  type: 'add-to-cart' | 'buy-now';
  sku: string;
  slug: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = `${environment.apiBase}/auth`;
  private readonly returnUrlKey = 'nexbuy.returnUrl';
  private readonly lastVisitedKey = 'nexbuy.lastVisitedUrl';
  private readonly pendingActionKey = 'nexbuy.pendingShopAction';
  private readonly authStateSubject = new BehaviorSubject<boolean>(this.hasStoredToken() && this.hasValidStoredToken());

  readonly authState$ = this.authStateSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(body: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/login`, this.normalizeAuthPayload(body)).pipe(tap((res) => this.storeAuth(res)));
  }

  register(body: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/register`, this.normalizeAuthPayload(body)).pipe(tap((res) => this.storeAuth(res)));
  }

  verifyOtp(body: { userId: number; otp: string; purpose: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/verify-otp`, body).pipe(tap((res) => this.storeAuth(res)));
  }

  resendOtp(body: { userId: number; purpose: string }): Observable<void> {
    return this.http.post<void>(`${this.api}/resend-otp`, body);
  }

  updatePendingRegistrationEmail(body: { userId: number; email: string }): Observable<void> {
    return this.http.post<void>(`${this.api}/update-registration-email`, {
      ...body,
      email: this.normalizeEmail(body.email)
    });
  }

  forgot(body: { email: string }): Observable<void> {
    return this.http.post<void>(`${this.api}/forgot`, { email: this.normalizeEmail(body.email) });
  }

  reset(body: { email: string; otp: string; newPassword: string; confirmPassword: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/reset`, {
      ...body,
      email: this.normalizeEmail(body.email)
    }).pipe(tap((res) => this.storeAuth(res)));
  }

  setAuth(res: AuthResponse): void {
    this.storeAuth(res);
  }

  logout(): void {
    this.clearStoredAuth();
  }

  isLoggedIn(): boolean {
    if (!this.hasStoredToken()) {
      return false;
    }
    if (!this.hasValidStoredToken()) {
      this.clearStoredAuth();
      return false;
    }
    return true;
  }

  isAdmin(): boolean {
    return (this.userRole || '').toUpperCase() === 'ADMIN';
  }

  get userRole(): string | null {
    return localStorage.getItem('userRole');
  }

  get token(): string | null {
    return this.isLoggedIn() ? localStorage.getItem('token') : null;
  }

  rememberReturnUrl(url: string): void {
    sessionStorage.setItem(this.returnUrlKey, this.sanitizeUrl(url));
  }

  consumeReturnUrl(): string | null {
    const value = sessionStorage.getItem(this.returnUrlKey);
    if (value) {
      sessionStorage.removeItem(this.returnUrlKey);
    }
    return value;
  }

  rememberLastVisitedUrl(url: string): void {
    const sanitized = this.sanitizeUrl(url);
    if (sanitized && !sanitized.startsWith('/auth')) {
      sessionStorage.setItem(this.lastVisitedKey, sanitized);
    }
  }

  getLastVisitedUrl(): string | null {
    return sessionStorage.getItem(this.lastVisitedKey);
  }

  setPendingShopAction(action: PendingShopAction): void {
    sessionStorage.setItem(this.pendingActionKey, JSON.stringify(action));
  }

  consumePendingShopAction(): PendingShopAction | null {
    const raw = sessionStorage.getItem(this.pendingActionKey);
    if (!raw) {
      return null;
    }
    sessionStorage.removeItem(this.pendingActionKey);
    try {
      return JSON.parse(raw) as PendingShopAction;
    } catch {
      return null;
    }
  }

  private storeAuth(res: AuthResponse): void {
    if (res.token) {
      localStorage.setItem('token', res.token);
      if (res.refreshToken) {
        localStorage.setItem('refreshToken', res.refreshToken);
      }
      localStorage.setItem('userEmail', res.email);
      if (res.role) {
        localStorage.setItem('userRole', res.role.toUpperCase());
      }
      this.authStateSubject.next(true);
    }
  }

  private clearStoredAuth(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRole');
    this.authStateSubject.next(false);
  }

  private hasStoredToken(): boolean {
    return !!localStorage.getItem('token');
  }

  private hasValidStoredToken(): boolean {
    const token = localStorage.getItem('token');
    if (!token) {
      return false;
    }

    const parts = token.split('.');
    if (parts.length !== 3) {
      return false;
    }

    try {
      const payload = JSON.parse(this.decodeBase64Url(parts[1])) as { exp?: number };
      if (!payload.exp) {
        return true;
      }
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private normalizeAuthPayload(body: any): any {
    if (!body || typeof body !== 'object') {
      return body;
    }

    const normalized = { ...body };
    if ('email' in normalized) {
      normalized.email = this.normalizeEmail(normalized.email);
    }
    return normalized;
  }

  private normalizeEmail(value: string | null | undefined): string {
    return (value || '').replace(/\s+/g, '').trim().toLowerCase();
  }

  private decodeBase64Url(value: string): string {
    const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
    const padding = normalized.length % 4;
    const padded = padding ? normalized + '='.repeat(4 - padding) : normalized;
    return atob(padded);
  }

  private sanitizeUrl(url: string): string {
    if (!url) {
      return '/product';
    }
    const [path, queryString] = url.split('?');
    if (!queryString) {
      return path;
    }
    const params = new URLSearchParams(queryString);
    params.delete('auth');
    const next = params.toString();
    return next ? `${path}?${next}` : path;
  }
}