import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { AddressSummary, OrderSummary, ProfileSummary } from '../order/order.service';

interface CheckoutProfileResponse {
  profile: ProfileSummary;
  addresses: AddressSummary[];
}

export interface UserDashboardResponse {
  profile: ProfileSummary;
  addresses: AddressSummary[];
  orders: OrderSummary[];
}

@Injectable({ providedIn: 'root' })
export class UserAccountService {
  private readonly usersApi = `${environment.apiBase}/users/me`;
  private readonly ordersApi = `${environment.apiBase}/orders`;

  constructor(private readonly http: HttpClient) {}

  getDashboard(): Observable<UserDashboardResponse> {
    return forkJoin({
      profilePayload: this.http.get<CheckoutProfileResponse>(`${this.usersApi}/checkout-profile`),
      orders: this.http.get<OrderSummary[]>(`${this.ordersApi}/me`)
    }).pipe(
      map(({ profilePayload, orders }) => ({
        profile: profilePayload.profile,
        addresses: profilePayload.addresses ?? [],
        orders: orders ?? []
      }))
    );
  }

  setDefaultAddress(addressId: number): Observable<AddressSummary> {
    return this.http.patch<AddressSummary>(`${this.usersApi}/addresses/${addressId}/default`, {});
  }
}