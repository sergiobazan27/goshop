import { DecimalPipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  Injector,
  OnInit,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, Subject, catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { LucideArmchair, LucideArrowLeft } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { OrderEditor } from '../../components/order-editor/order-editor';
import { OrderHistory } from '../../components/order-history/order-history';
import { keycloak } from '../../auth/keycloak';
import { DiningOrder, TableOrder } from '../../core/models/order.model';
import { CurrentUser } from '../../core/models/current-user.model';
import { OrderService } from '../../core/services/order.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import { orderError } from '../../core/errors/order-error';

@Component({
  selector: 'app-orders',
  imports: [
    DecimalPipe,
    RouterLink,
    Header,
    Sidebar,
    OrderEditor,
    OrderHistory,
    LucideArmchair,
    LucideArrowLeft,
  ],
  templateUrl: './orders.html',
  styleUrls: ['../../shared/styles/admin-panel.css', './orders.css'],
})
export class Orders implements OnInit {
  private readonly service = inject(OrderService);
  private readonly profiles = inject(CurrentUserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly requests = new Subject<number>();
  private readonly route = inject(ActivatedRoute);
  private initialOrderId = this.route.snapshot.queryParamMap.get('orderId');
  readonly isOwner = keycloak.hasRealmRole('owner');
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Mi cuenta';
  readonly user = signal<CurrentUser | null>(null);
  readonly tables = signal<TableOrder[]>([]);
  readonly highlightedTableId = signal(this.route.snapshot.queryParamMap.get('tableId'));
  readonly highlightedTable = computed(() =>
    this.tables().find((entry) => entry.table.id === this.highlightedTableId()),
  );
  readonly selected = signal<DiningOrder | null>(null);
  readonly editor = viewChild(OrderEditor);
  readonly history = viewChild.required(OrderHistory);
  readonly loading = signal(true);
  readonly opening = signal(false);
  readonly editing = signal(false);
  readonly error = signal('');
  readonly detailError = signal('');
  readonly page = signal(0);
  readonly total = signal(0);
  readonly totalPages = signal(0);
  readonly size = 12;

  ngOnInit() {
    this.requests
      .pipe(
        switchMap((page) => {
          this.loading.set(true);
          this.error.set('');
          return forkJoin({
            tables: this.service.tables(page, this.size),
            user: this.profiles.getMe(),
          }).pipe(
            map((data) => ({ data, error: '' })),
            catchError((error: unknown) => of({ data: null, error: orderError(error) })),
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
        if (data && this.initialOrderId) {
          const id = this.initialOrderId;
          this.initialOrderId = null;
          this.selectOrder(this.service.get(id));
        }
      });
    this.load();
  }

  load() {
    this.requests.next(this.page());
  }

  goToPage(page: number) {
    if (this.loading() || this.opening() || this.editing() || page < 0 || page >= this.totalPages())
      return;
    this.page.set(page);
    this.load();
  }

  backToTables() {
    if (this.opening() || this.editing()) return;
    this.selected.set(null);
    this.detailError.set('');
  }

  choose(table: TableOrder) {
    if (this.loading() || this.opening() || this.editing()) return;
    this.highlightedTableId.set(null);
    const operation = table.orderId
      ? this.service.get(table.orderId)
      : this.service.open(table.table.id);
    this.selectOrder(operation);
  }

  viewClosedOrder(id: string) {
    if (this.loading() || this.opening() || this.editing()) return;
    this.selectOrder(this.service.get(id));
  }

  private selectOrder(operation: Observable<DiningOrder>) {
    this.opening.set(true);
    this.detailError.set('');
    operation
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.opening.set(false)),
      )
      .subscribe({
        next: (order) => {
          this.selected.set(order);
          this.load();
          afterNextRender(
            () => {
              window.scrollTo({ top: 0 });
              this.editor()?.focus();
            },
            { injector: this.injector },
          );
        },
        error: (error: unknown) => {
          this.detailError.set(orderError(error));
          this.load();
        },
      });
  }

  reloadOrder() {
    const order = this.selected();
    if (!order || this.opening() || this.editing()) return;
    this.opening.set(true);
    this.detailError.set('');
    this.service
      .get(order.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.opening.set(false)),
      )
      .subscribe({
        next: (updated) => {
          this.selected.set(updated);
          this.load();
        },
        error: (error: unknown) => this.detailError.set(orderError(error)),
      });
  }

  orderSaved(order: DiningOrder) {
    this.selected.set(order);
    this.load();
    if (order.status === 'CLOSED') this.history().load();
  }
}
