import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { LucidePlus } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { TeamMemberDialog } from '../../components/team-member-dialog/team-member-dialog';
import { keycloak } from '../../auth/keycloak';
import { TeamMember } from '../../core/models/team-member.model';
import { CurrentUser } from '../../core/models/current-user.model';
import { TeamService } from '../../core/services/team.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import { teamError } from '../../core/errors/team-error';

@Component({
  selector: 'app-team',
  imports: [DatePipe, Header, Sidebar, TeamMemberDialog, LucidePlus],
  templateUrl: './team.html',
  styleUrl: '../../shared/styles/admin-panel.css',
})
export class Team implements OnInit {
  private readonly service = inject(TeamService);
  private readonly profiles = inject(CurrentUserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<number>();
  readonly editor = viewChild.required(TeamMemberDialog);
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Mi cuenta';
  readonly user = signal<CurrentUser | null>(null);
  readonly members = signal<TeamMember[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly success = signal('');
  readonly page = signal(0);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly size = 10;

  ngOnInit() {
    this.requests
      .pipe(
        switchMap((page) => {
          this.loading.set(true);
          this.error.set('');
          return forkJoin({
            members: this.service.list(page, this.size),
            user: this.profiles.getMe(),
          }).pipe(
            map((data) => ({ data, error: '' })),
            catchError((error: unknown) => of({ data: null, error: teamError(error) })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ data, error }) => {
        if (data && data.members.totalPages > 0 && data.members.page >= data.members.totalPages) {
          this.page.set(data.members.totalPages - 1);
          this.load();
          return;
        }
        this.members.set(data?.members.items ?? []);
        this.user.set(data?.user ?? null);
        this.total.set(data?.members.total ?? 0);
        this.totalPages.set(data?.members.totalPages ?? 0);
        if (data) this.page.set(data.members.page);
        this.error.set(error);
        this.loading.set(false);
      });
    this.load();
  }

  load() {
    this.requests.next(this.page());
  }

  canAdd() {
    const subscription = this.user()?.subscription;
    return (
      !this.loading() &&
      !this.error() &&
      !!subscription &&
      this.total() < subscription.plan.maxUsers
    );
  }

  displayName(member: TeamMember) {
    return member.username ?? (member.role === 'OWNER' ? this.username : member.keycloakUserId);
  }

  goToPage(page: number) {
    if (this.loading() || page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
  }

  teamChanged(message: string) {
    this.success.set(message);
    this.load();
  }
}
