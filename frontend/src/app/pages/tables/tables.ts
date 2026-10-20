import { Component, DestroyRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { LucidePlus } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { DiningTableForm } from '../../components/dining-table-form/dining-table-form';
import { keycloak } from '../../auth/keycloak';
import { DiningTable } from '../../core/models/dining-table.model';
import { CurrentUser } from '../../core/models/current-user.model';
import { DiningTableService } from '../../core/services/dining-table.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import { diningTableError } from '../../core/errors/dining-table-error';

@Component({
  selector: 'app-tables',
  imports: [Header, Sidebar, DiningTableForm, LucidePlus],
  templateUrl: './tables.html',
  styleUrl: '../../shared/styles/admin-panel.css',
})
export class Tables implements OnInit {
  private readonly service = inject(DiningTableService);
  private readonly profiles = inject(CurrentUserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<number>();
  readonly editor = viewChild.required(DiningTableForm);
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Mi cuenta';
  readonly user = signal<CurrentUser | null>(null);
  readonly tables = signal<DiningTable[]>([]);
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
            tables: this.service.list(page, this.size),
            user: this.profiles.getMe(),
          }).pipe(
            map((data) => ({ data, error: '' })),
            catchError((error: unknown) => of({ data: null, error: diningTableError(error) })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ data, error }) => {
        if (data && data.tables.totalPages > 0 && data.tables.page >= data.tables.totalPages) {
          this.page.set(data.tables.totalPages - 1);
          this.load();
          return;
        }
        this.tables.set(data?.tables.items ?? []);
        this.user.set(data?.user ?? null);
        this.total.set(data?.tables.total ?? 0);
        this.totalPages.set(data?.tables.totalPages ?? 0);
        if (data) this.page.set(data.tables.page);
        this.error.set(error);
        this.loading.set(false);
      });
    this.load();
  }

  load() {
    this.requests.next(this.page());
  }

  canCreate() {
    const subscription = this.user()?.subscription;
    return (
      !this.loading() &&
      !this.error() &&
      !!subscription &&
      this.total() < subscription.plan.maxTables
    );
  }

  goToPage(page: number) {
    if (this.loading() || page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
  }

  tableSaved(message: string) {
    this.success.set(message);
    this.load();
  }
}
