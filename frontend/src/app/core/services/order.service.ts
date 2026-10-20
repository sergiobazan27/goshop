import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  CloseOrderRequest,
  DiningOrder,
  OrderHistoryEntry,
  OrderItemRequest,
  TableOrder,
} from '../models/order.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/orders`;

  tables(page = 0, size = 12) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<TableOrder>>(`${this.endpoint}/tables`, { params });
  }

  open(tableId: string) {
    return this.http.post<DiningOrder>(this.endpoint, { tableId });
  }

  history(page = 0, size = 10) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<OrderHistoryEntry>>(`${this.endpoint}/history`, { params });
  }

  close(id: string, request: CloseOrderRequest) {
    return this.http.put<DiningOrder>(`${this.endpoint}/${encodeURIComponent(id)}/close`, request);
  }

  get(id: string) {
    return this.http.get<DiningOrder>(`${this.endpoint}/${encodeURIComponent(id)}`);
  }

  setQuantity(id: string, productId: string, request: OrderItemRequest) {
    return this.http.put<DiningOrder>(
      `${this.endpoint}/${encodeURIComponent(id)}/items/${encodeURIComponent(productId)}`,
      request,
    );
  }
}
