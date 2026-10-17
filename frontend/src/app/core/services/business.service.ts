import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Business, CreateBusinessRequest } from '../models/business.model';

@Injectable({ providedIn: 'root' })
export class BusinessService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/admin/businesses`;
  private readonly created = signal<Business[]>([]);
  readonly createdBusinesses = this.created.asReadonly();

  create(request: CreateBusinessRequest) {
    return this.http
      .post<Business>(this.endpoint, request)
      .pipe(tap((business) => this.created.update((businesses) => [business, ...businesses])));
  }
}
