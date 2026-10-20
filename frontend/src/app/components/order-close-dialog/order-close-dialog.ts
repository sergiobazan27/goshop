import { DecimalPipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { DiningOrder } from '../../core/models/order.model';
import { OrderService } from '../../core/services/order.service';
import { orderError } from '../../core/errors/order-error';

@Component({
  selector: 'app-order-close-dialog',
  imports: [DecimalPipe],
  templateUrl: './order-close-dialog.html',
  styleUrl: '../../shared/styles/admin-form.css',
})
export class OrderCloseDialog {
  private readonly service = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly order = signal<DiningOrder | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly closed = output<DiningOrder>();
  readonly busy = output<boolean>();

  open(order: DiningOrder) {
    if (this.saving() || order.status !== 'OPEN') return;
    this.order.set(order);
    this.error.set('');
    this.dialog().nativeElement.showModal();
  }

  dismiss() {
    if (!this.saving()) this.dialog().nativeElement.close();
  }

  cancel(event: Event) {
    if (this.saving()) event.preventDefault();
  }

  confirm() {
    const order = this.order();
    if (!order || this.saving()) return;
    this.saving.set(true);
    this.busy.emit(true);
    this.error.set('');
    this.service
      .close(order.id, { revision: order.revision })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving.set(false);
          if (!this.destroyRef.destroyed) this.busy.emit(false);
        }),
      )
      .subscribe({
        next: (closed) => {
          this.closed.emit(closed);
          this.dialog().nativeElement.close();
        },
        error: (error: unknown) => this.error.set(orderError(error)),
      });
  }
}
