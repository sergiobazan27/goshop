import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Observable, forkJoin } from 'rxjs';
import {
  AssignSubscriptionRequest,
  RenewSubscriptionRequest,
  Subscription,
  SubscriptionHistoryEntry,
  SubscriptionDetails,
} from '../models/subscription.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/admin/businesses`;
  private url(businessId: string) {
    return `${this.endpoint}/${encodeURIComponent(businessId)}/subscription`;
  }

  current(businessId: string) {
    return this.http.get<Subscription | null>(this.url(businessId));
  }
  details(businessId: string): Observable<SubscriptionDetails> {
    return forkJoin({ current: this.current(businessId), history: this.history(businessId) });
  }
  history(businessId: string, page = 0) {
    return this.http.get<Page<SubscriptionHistoryEntry>>(`${this.url(businessId)}/history`, {
      params: { page, size: 10 },
    });
  }
  assign(businessId: string, request: AssignSubscriptionRequest) {
    return this.http.post<Subscription>(this.url(businessId), request);
  }
  renew(businessId: string, request: RenewSubscriptionRequest) {
    return this.http.post<Subscription>(`${this.url(businessId)}/renewals`, request);
  }
  changePlan(businessId: string, planId: string) {
    return this.http.put<Subscription>(`${this.url(businessId)}/plan`, { planId });
  }
}
