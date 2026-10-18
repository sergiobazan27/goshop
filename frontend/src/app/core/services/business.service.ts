import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Business, BusinessListQuery, CreateBusinessRequest } from '../models/business.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class BusinessService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/admin/businesses`;

  list(query: BusinessListQuery) {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search.trim()) params = params.set('search', query.search.trim());
    if (query.status) params = params.set('status', query.status);
    return this.http.get<Page<Business>>(this.endpoint, { params });
  }

  create(request: CreateBusinessRequest) {
    return this.http.post<Business>(this.endpoint, request);
  }
}
