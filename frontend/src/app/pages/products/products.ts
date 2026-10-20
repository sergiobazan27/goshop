import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { LucidePlus } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { ProductForm } from '../../components/product-form/product-form';
import { keycloak } from '../../auth/keycloak';
import { Product } from '../../core/models/product.model';
import { CurrentUser } from '../../core/models/current-user.model';
import { ProductService } from '../../core/services/product.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import { productError } from '../../core/errors/product-error';

@Component({
  selector: 'app-products',
  imports: [DecimalPipe, Header, Sidebar, ProductForm, LucidePlus],
  templateUrl: './products.html',
  styleUrl: '../../shared/styles/admin-panel.css',
})
export class Products implements OnInit {
  private readonly service = inject(ProductService);
  private readonly profiles = inject(CurrentUserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<number>();
  readonly editor = viewChild(ProductForm);
  readonly canManage = keycloak.hasRealmRole('owner');
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Mi cuenta';
  readonly user = signal<CurrentUser | null>(null);
  readonly products = signal<Product[]>([]);
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
            products: this.service.list(page, this.size),
            user: this.profiles.getMe(),
          }).pipe(
            map((data) => ({ data, error: '' })),
            catchError((error: unknown) => of({ data: null, error: productError(error) })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ data, error }) => {
        if (
          data &&
          data.products.totalPages > 0 &&
          data.products.page >= data.products.totalPages
        ) {
          this.page.set(data.products.totalPages - 1);
          this.load();
          return;
        }
        this.products.set(data?.products.items ?? []);
        this.user.set(data?.user ?? null);
        this.total.set(data?.products.total ?? 0);
        this.totalPages.set(data?.products.totalPages ?? 0);
        if (data) this.page.set(data.products.page);
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
      this.canManage &&
      !this.loading() &&
      !this.error() &&
      !!subscription &&
      this.total() < subscription.plan.maxProducts
    );
  }

  goToPage(page: number) {
    if (this.loading() || page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.load();
  }

  productSaved(product: Product) {
    this.success.set(`${product.name} se guardó correctamente.`);
    this.load();
  }
}
