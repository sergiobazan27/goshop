import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { DiningTable, DiningTableRequest } from '../models/dining-table.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class DiningTableService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/tables`;

  list(page = 0, size = 10) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<DiningTable>>(this.endpoint, { params });
  }

  create(request: DiningTableRequest) {
    return this.http.post<DiningTable>(this.endpoint, request);
  }

  update(id: string, request: DiningTableRequest) {
    return this.http.put<DiningTable>(`${this.endpoint}/${encodeURIComponent(id)}`, request);
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.endpoint}/${encodeURIComponent(id)}`);
  }
}
