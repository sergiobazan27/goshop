import { Component, DestroyRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, map, of, switchMap } from 'rxjs';
import { LucidePlus, LucideSearch } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { PlanForm } from '../../components/plan-form/plan-form';
import { keycloak } from '../../auth/keycloak';
import { Plan, PlanListQuery } from '../../core/models/plan.model';
import { PlanService } from '../../core/services/plan.service';
import { planError } from '../../core/errors/plan-error';

@Component({
  selector: 'app-plans',
  imports: [Header, Sidebar, PlanForm, LucidePlus, LucideSearch],
  templateUrl: './plans.html',
  styleUrl: '../../shared/styles/admin-panel.css',
})
export class Plans implements OnInit {
  private readonly service = inject(PlanService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<PlanListQuery>();
  private filters: Pick<PlanListQuery, 'search' | 'active'> = { search: '', active: '' };
  readonly editor = viewChild.required(PlanForm);
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Administrador';
  readonly search = signal('');
  readonly active = signal<PlanListQuery['active']>('');
  readonly plans = signal<Plan[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly success = signal('');
  readonly page = signal(0);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly hasFilters = signal(false);
  readonly size = 10;

  ngOnInit() {
    this.requests
      .pipe(
        switchMap((query) => {
          this.loading.set(true);
          this.error.set('');
          this.hasFilters.set(Boolean(query.search || query.active));
          return this.service.list(query).pipe(
            map((data) => ({ data, error: '' })),
            catchError((error: unknown) => of({ data: null, error: planError(error) })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ data, error }) => {
        if (data && data.totalPages > 0 && data.page >= data.totalPages) {
          this.page.set(data.totalPages - 1);
          this.load();
          return;
        }
        this.plans.set(data?.items ?? []);
        this.total.set(data?.total ?? 0);
        this.totalPages.set(data?.totalPages ?? 0);
        if (data) this.page.set(data.page);
        this.error.set(error);
        this.loading.set(false);
      });
    this.load();
  }

  load() {
    this.requests.next({ ...this.filters, page: this.page(), size: this.size });
  }
  applyFilters(event?: Event) {
    event?.preventDefault();
    this.filters = { search: this.search().trim(), active: this.active() };
    this.page.set(0);
    this.load();
  }
  clearFilters() {
    this.search.set('');
    this.active.set('');
    this.applyFilters();
  }
  goToPage(page: number) {
    if (this.loading() || page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
  }
  planSaved(plan: Plan) {
    this.success.set(`${plan.name} se guardó correctamente.`);
    this.page.set(0);
    this.load();
  }
}
