import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, catchError, map, of, switchMap } from 'rxjs';
import { LucideArrowRight, LucidePackage, LucidePlus } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { ServiceSummary } from '../../components/service-summary/service-summary';
import { TablesOverview } from '../../components/tables-overview/tables-overview';
import { RecentOrders } from '../../components/recent-orders/recent-orders';
import { OrdersTable } from '../../components/orders-table/orders-table';
import { Footer } from '../../components/footer/footer';
import { CurrentUserService } from '../../core/services/current-user.service';
import { OverviewService } from '../../core/services/overview.service';
import { CurrentUser } from '../../core/models/current-user.model';
import { Overview } from '../../core/models/overview.model';
import { overviewError } from '../../core/errors/overview-error';
import { keycloak } from '../../auth/keycloak';

@Component({
  imports: [
    DatePipe,
    RouterLink,
    Header,
    Sidebar,
    ServiceSummary,
    TablesOverview,
    RecentOrders,
    OrdersTable,
    Footer,
    LucidePlus,
    LucideArrowRight,
    LucidePackage,
  ],
  selector: 'app-home',
  styleUrl: './home.css',
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private readonly profiles = inject(CurrentUserService);
  private readonly service = inject(OverviewService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<void>();
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Mi cuenta';
  readonly isOwner = keycloak.hasRealmRole('owner');
  readonly user = signal<CurrentUser | null>(null);
  readonly overview = signal<Overview | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly canOperate = computed(() => {
    const user = this.user();
    return (
      !!user?.membershipRole &&
      user.business?.status === 'ACTIVE' &&
      user.subscription?.status === 'ACTIVE' &&
      user.roles.some((role) => role === 'owner' || role === 'waiter')
    );
  });
  readonly dateLabel = computed(() =>
    new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'long' }).format(
      new Date(this.overview()?.generatedAt ?? Date.now()),
    ),
  );

  ngOnInit() {
    this.requests
      .pipe(
        switchMap(() => {
          this.loading.set(true);
          this.error.set('');
          this.overview.set(null);
          return this.profiles.getMe().pipe(
            switchMap((user) => {
              this.user.set(user);
              if (!this.canOperate()) return of({ data: null, error: '' });
              return this.service.get().pipe(
                map((data) => ({ data, error: '' })),
                catchError((error: unknown) => of({ data: null, error: overviewError(error) })),
              );
            }),
            catchError((error: unknown) => {
              this.user.set(null);
              return of({ data: null, error: overviewError(error) });
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ data, error }) => {
        this.overview.set(data);
        this.error.set(error);
        this.loading.set(false);
      });
    this.load();
  }

  load() {
    this.requests.next();
  }
}
