import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AddTeamMemberRequest, AvailableWaiter, TeamMember } from '../models/team-member.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/team`;

  list(page = 0, size = 10) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<TeamMember>>(this.endpoint, { params });
  }

  add(request: AddTeamMemberRequest) {
    return this.http.post<TeamMember>(this.endpoint, request);
  }

  availableWaiters(search = '', page = 0, size = 10) {
    const params = new HttpParams()
      .set('search', search.trim())
      .set('page', page)
      .set('size', size);
    return this.http.get<Page<AvailableWaiter>>(`${this.endpoint}/available-waiters`, { params });
  }

  remove(id: string) {
    return this.http.delete<void>(`${this.endpoint}/${encodeURIComponent(id)}`);
  }
}
