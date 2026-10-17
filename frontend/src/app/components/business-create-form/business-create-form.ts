import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideX } from '@lucide/angular';
import { finalize } from 'rxjs';
import { Business } from '../../core/models/business.model';
import { BusinessService } from '../../core/services/business.service';
import { businessCreationError } from '../../core/errors/business-creation-error';

@Component({
  selector: 'app-business-create-form',
  imports: [ReactiveFormsModule, LucideX],
  templateUrl: './business-create-form.html',
  styleUrl: './business-create-form.css',
})
export class BusinessCreateForm {
  private readonly businessService = inject(BusinessService);
  private readonly builder = inject(NonNullableFormBuilder);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly created = output<Business>();
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = this.builder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)]],
    ownerKeycloakUserId: [
      '',
      [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)],
    ],
  });

  open() {
    if (this.saving()) return;
    this.form.reset();
    this.error.set('');
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
    if (this.form.invalid) return;
    const values = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');
    this.form.disable();
    this.businessService
      .create({
        name: values.name.trim(),
        ownerKeycloakUserId: values.ownerKeycloakUserId.trim(),
      })
      .pipe(
        finalize(() => {
          this.saving.set(false);
          this.form.enable();
        }),
      )
      .subscribe({
        next: (business) => {
          this.created.emit(business);
          this.dialog().nativeElement.close();
        },
        error: (error: unknown) => this.error.set(businessCreationError(error)),
      });
  }
}
