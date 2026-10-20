import { DatePipe, DecimalPipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  OnChanges,
  SimpleChanges,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, Subscription, debounceTime, finalize } from 'rxjs';
import {
  LucidePlus,
  LucideMinus,
  LucideReceiptText,
  LucideSearch,
  LucideLeaf,
} from '@lucide/angular';
import { DiningOrder } from '../../core/models/order.model';
import { Page } from '../../core/models/page.model';
import { Product } from '../../core/models/product.model';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { orderError } from '../../core/errors/order-error';
import { OrderCloseDialog } from '../order-close-dialog/order-close-dialog';
import { OrderProductCard } from '../order-product-card/order-product-card';

@Component({
  selector: 'app-order-editor',
  imports: [
    DatePipe,
    DecimalPipe,
    OrderCloseDialog,
    OrderProductCard,
    LucidePlus,
    LucideMinus,
    LucideReceiptText,
    LucideSearch,
    LucideLeaf,
  ],
  templateUrl: './order-editor.html',
  styleUrls: ['../../shared/styles/admin-panel.css', './order-editor.css'],
  host: { tabindex: '-1', 'aria-label': 'Detalle del pedido' },
})
export class OrderEditor implements OnChanges {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly service = inject(OrderService);
  private readonly catalog = inject(ProductService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searches = new Subject<void>();
  private productsRequest?: Subscription;
  readonly order = input.required<DiningOrder>();
  readonly closeDialog = viewChild.required(OrderCloseDialog);
  readonly disabled = input(false);
  readonly saved = output<DiningOrder>();
  readonly reload = output<void>();
  readonly busy = output<boolean>();
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly products = signal<Page<Product> | null>(null);
  readonly productsLoading = signal(false);
  readonly productsError = signal('');
  readonly search = signal('');
  readonly available = signal<boolean | null>(null);
  readonly quantity = computed(() =>
    this.order().items.reduce((sum, item) => sum + item.quantity, 0),
  );

  constructor() {
    this.searches
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadProducts());
  }

  searchProducts(value: string) {
    this.search.set(value);
    // Cancel the previous response immediately so it cannot replace a newer search.
    this.productsRequest?.unsubscribe();
    this.productsLoading.set(true);
    this.searches.next();
  }

  filterProducts(available: boolean | null) {
    if (this.saving() || this.disabled()) return;
    this.available.set(available);
    this.loadProducts();
  }

  ngOnChanges(changes: SimpleChanges) {
    const change = changes['order'];
    if (
      change &&
      change.previousValue?.id !== change.currentValue.id &&
      change.currentValue.status === 'OPEN'
    ) {
      this.error.set('');
      this.success.set('');
      this.loadProducts();
    }
  }

  loadProducts(page = 0) {
    if (page < 0 || this.order().status !== 'OPEN') return;
    this.productsRequest?.unsubscribe();
    this.productsLoading.set(true);
    this.productsError.set('');
    this.productsRequest = this.catalog
      .list(page, 12, this.search(), this.available())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (products) => {
          if (products.page > 0 && products.page >= products.totalPages) {
            this.loadProducts(Math.max(0, products.totalPages - 1));
            return;
          }
          this.products.set(products);
          this.productsLoading.set(false);
        },
        error: (error: unknown) => {
          this.products.set(null);
          this.productsError.set(orderError(error));
          this.productsLoading.set(false);
        },
      });
  }

  productQuantity(productId: string) {
    return this.order().items.find((item) => item.productId === productId)?.quantity ?? 0;
  }

  add(product: Product) {
    if (this.productsLoading() || !product.available) return;
    this.save(product.id, this.productQuantity(product.id) + 1);
  }

  refresh() {
    if (this.saving() || this.disabled()) return;
    this.error.set('');
    this.success.set('');
    this.reload.emit();
  }

  save(productId: string, quantity: number) {
    if (this.saving() || this.disabled() || this.order().status !== 'OPEN') return;
    if (!Number.isInteger(quantity) || quantity < 0 || quantity > 999) {
      this.error.set('Usa una cantidad entera de 1 a 999, o cero para quitar el producto.');
      return;
    }
    this.saving.set(true);
    this.busy.emit(true);
    this.error.set('');
    this.success.set('');
    const order = this.order();
    this.service
      .setQuantity(order.id, productId, { quantity, revision: order.revision })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving.set(false);
          if (!this.destroyRef.destroyed) this.busy.emit(false);
        }),
      )
      .subscribe({
        next: (updated) => {
          this.success.set('Pedido guardado.');
          this.saved.emit(updated);
        },
        error: (error: unknown) => this.error.set(orderError(error)),
      });
  }

  closingChanged(busy: boolean) {
    this.saving.set(busy);
    this.busy.emit(busy);
  }

  orderClosed(order: DiningOrder) {
    this.error.set('');
    this.success.set('Pedido cerrado. Se conservó su detalle en el historial.');
    this.saved.emit(order);
  }

  focus() {
    this.host.nativeElement.focus({ preventScroll: true });
  }
}
