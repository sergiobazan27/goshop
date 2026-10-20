import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Product, ProductRequest } from '../models/product.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/products`;

  list(page = 0, size = 10, search = '', available: boolean | null = null) {
    let params = new HttpParams().set('page', page).set('size', size).set('search', search);
    if (available !== null) params = params.set('available', available);
    return this.http.get<Page<Product>>(this.endpoint, { params });
  }

  create(request: ProductRequest) {
    return this.http.post<Product>(this.endpoint, request);
  }

  update(id: string, request: ProductRequest) {
    return this.http.put<Product>(`${this.endpoint}/${encodeURIComponent(id)}`, request);
  }
}
