import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Page } from '../models/page.model';
import { Plan, PlanListQuery, PlanRequest } from '../models/plan.model';

@Injectable({ providedIn: 'root' })
export class PlanService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/admin/plans`;

  list(query: PlanListQuery) {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search.trim()) params = params.set('search', query.search.trim());
    if (query.active) params = params.set('active', query.active);
    return this.http.get<Page<Plan>>(this.endpoint, { params });
  }

  create(request: PlanRequest) {
    return this.http.post<Plan>(this.endpoint, request);
  }

  update(id: string, request: PlanRequest) {
    return this.http.put<Plan>(`${this.endpoint}/${encodeURIComponent(id)}`, request);
  }
}
