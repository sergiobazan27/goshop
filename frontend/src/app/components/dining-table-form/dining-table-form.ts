import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideX } from '@lucide/angular';
import { Observable, finalize } from 'rxjs';
import { DiningTable } from '../../core/models/dining-table.model';
import { DiningTableService } from '../../core/services/dining-table.service';
import { diningTableError } from '../../core/errors/dining-table-error';

@Component({
  selector: 'app-dining-table-form',
  imports: [ReactiveFormsModule, LucideX],
  templateUrl: './dining-table-form.html',
  styleUrl: '../../shared/styles/admin-form.css',
})
export class DiningTableForm {
  private readonly service = inject(DiningTableService);
  private readonly builder = inject(FormBuilder);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly editing = signal<DiningTable | null>(null);
  readonly deleting = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly saved = output<string>();
  readonly form = this.builder.group({
    name: this.builder.nonNullable.control('', [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(100),
    ]),
    capacity: this.builder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(2147483647),
      Validators.pattern(/^[1-9]\d*$/),
    ]),
  });

  open(table: DiningTable | null = null, deleting = false) {
    if (this.saving() || (deleting && !table)) return;
    this.editing.set(table);
    this.deleting.set(deleting);
    this.error.set('');
    this.form.reset({ name: table?.name ?? '', capacity: table?.capacity ?? null });
    this.dialog().nativeElement.showModal();
  }

  close() {
    if (!this.saving()) this.dialog().nativeElement.close();
  }

  cancel(event: Event) {
    if (this.saving()) event.preventDefault();
  }

  submit() {
    if (this.saving()) return;
    this.form.markAllAsTouched();
    const editing = this.editing();
    const deleting = this.deleting();
    const value = this.form.getRawValue();
    if (!deleting && (this.form.invalid || value.capacity === null)) return;
    if (deleting && !editing) return;
    const request = { name: value.name.trim(), capacity: value.capacity! };
    const operation: Observable<DiningTable | void> = deleting
      ? this.service.delete(editing!.id)
      : editing
        ? this.service.update(editing.id, request)
        : this.service.create(request);
    this.saving.set(true);
    this.error.set('');
    this.form.disable();
    operation
      .pipe(
        finalize(() => {
          this.saving.set(false);
          this.form.enable();
        }),
      )
      .subscribe({
        next: () => {
          this.saved.emit(
            deleting ? `Se eliminó ${editing!.name}.` : `${request.name} se guardó correctamente.`,
          );
          this.dialog().nativeElement.close();
        },
        error: (error: unknown) => this.error.set(diningTableError(error)),
      });
  }
}
