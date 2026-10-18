import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, map, of, switchMap } from 'rxjs';
import {
  LucidePlus,
  LucideSearch,
  LucideStore,
  LucideCheck,
  LucideShieldCheck,
} from '@lucide/angular';
import { keycloak } from '../../auth/keycloak';
import { BusinessCreateForm } from '../../components/business-create-form/business-create-form';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { Business, BusinessListQuery } from '../../core/models/business.model';
import { businessListError } from '../../core/errors/business-list-error';
import { BusinessService } from '../../core/services/business.service';

@Component({
  selector: 'app-admin',
  imports: [
    DatePipe,
    Header,
    Sidebar,
    BusinessCreateForm,
    LucidePlus,
    LucideSearch,
    LucideStore,
    LucideCheck,
    LucideShieldCheck,
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin implements OnInit {
  private readonly businessService = inject(BusinessService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<BusinessListQuery>();
  private filters: Pick<BusinessListQuery, 'search' | 'status'> = { search: '', status: '' };
  readonly createForm = viewChild.required(BusinessCreateForm);
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Administrador';
  readonly businesses = signal<Business[]>([]);
  readonly search = signal('');
  readonly status = signal<Business['status'] | ''>('');
  readonly success = signal('');
  readonly loading = signal(true);
  readonly error = signal('');
  readonly page = signal(0);
  readonly size = 10;
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly hasFilters = signal(false);

  ngOnInit() {
    this.requests
      .pipe(
        switchMap((query) => {
          this.loading.set(true);
          this.error.set('');
          this.hasFilters.set(Boolean(query.search.trim() || query.status));
          return this.businessService.list(query).pipe(
            map((data) => ({ data, error: '' })),
            catchError((error: unknown) => of({ data: null, error: businessListError(error) })),
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
        this.businesses.set(data?.items ?? []);
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
    this.filters = { search: this.search().trim(), status: this.status() };
    this.page.set(0);
    this.load();
  }

  goToPage(page: number) {
    if (this.loading() || page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
  }

  businessCreated(business: Business) {
    this.search.set('');
    this.status.set('');
    this.success.set(`${business.name} se creó correctamente. Su dueño ya está vinculado.`);
    this.applyFilters();
  }

  clearFilters() {
    this.search.set('');
    this.status.set('');
    this.applyFilters();
  }
}
