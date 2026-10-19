import { Component, ElementRef, computed, inject, output, signal, viewChild } from '@angular/core';
import { finalize } from 'rxjs';
import { Business } from '../../core/models/business.model';
import { BusinessService } from '../../core/services/business.service';
import { businessStatusError } from '../../core/errors/business-status-error';

@Component({
  selector: 'app-business-status-dialog',
  templateUrl: './business-status-dialog.html',
})
export class BusinessStatusDialog {
  private readonly service = inject(BusinessService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly updated = output<Business>();
  readonly business = signal<Business | null>(null);
  readonly suspending = computed(() => this.business()?.status === 'ACTIVE');
  readonly saving = signal(false);
  readonly error = signal('');

  open(business: Business) {
    if (this.saving()) return;
    this.business.set(business);
    this.error.set('');
    this.dialog().nativeElement.showModal();
  }

  close() {
    if (!this.saving()) this.dialog().nativeElement.close();
  }

  cancel(event: Event) {
    if (this.saving()) event.preventDefault();
  }

  confirm() {
    const business = this.business();
    if (!business || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.service
      .changeStatus(business.id, { status: this.suspending() ? 'SUSPENDED' : 'ACTIVE' })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => {
          this.updated.emit(updated);
          this.dialog().nativeElement.close();
        },
        error: (error: unknown) => this.error.set(businessStatusError(error)),
      });
  }
}
