import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { OrderDetail } from '../order/order.service';

export interface CompletePaymentRequest {
  outcome: 'success' | 'failed';
  providerOrderId?: string;
  providerPaymentId?: string;
  signature?: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly api = `${environment.apiBase}/payments`;

  constructor(private readonly http: HttpClient) {}

  getPayment(orderNumber: string): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${this.api}/${encodeURIComponent(orderNumber)}`);
  }

  completePayment(orderNumber: string, body: CompletePaymentRequest): Observable<{ order: OrderDetail }> {
    return this.http.post<{ order: OrderDetail }>(`${this.api}/${encodeURIComponent(orderNumber)}/complete`, body);
  }

  cancelPayment(orderNumber: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${encodeURIComponent(orderNumber)}`);
  }
}