import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { CurrentUser } from '../models/current-user.model';

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/me`;

  getMe() {
    return this.http.get<CurrentUser>(this.endpoint);
  }
}
