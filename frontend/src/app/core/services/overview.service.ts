import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Overview } from '../models/overview.model';

@Injectable({ providedIn: 'root' })
export class OverviewService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/overview`;

  get() {
    return this.http.get<Overview>(this.endpoint);
  }
}
