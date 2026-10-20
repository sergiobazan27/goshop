import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { OrderHistoryEntry } from '../../core/models/order.model';
import { Page } from '../../core/models/page.model';
import { OrderService } from '../../core/services/order.service';
import { orderError } from '../../core/errors/order-error';

@Component({
  selector: 'app-order-history',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './order-history.html',
  styleUrl: '../../shared/styles/admin-panel.css',
})
export class OrderHistory implements OnInit {
  private readonly service = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  readonly disabled = input(false);
  readonly selected = output<string>();
  readonly history = signal<Page<OrderHistoryEntry> | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  ngOnInit() {
    this.load();
  }

  load(page = 0) {
    if (page < 0) return;
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set('');
    this.request = this.service
      .history(page)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (history) => {
          if (history.totalPages > 0 && history.page >= history.totalPages) {
            this.load(history.totalPages - 1);
            return;
          }
          this.history.set(history);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.error.set(orderError(error));
          this.loading.set(false);
        },
      });
  }
}
