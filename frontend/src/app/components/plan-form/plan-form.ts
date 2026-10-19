import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideX } from '@lucide/angular';
import { finalize } from 'rxjs';
import { Plan, PlanRequest } from '../../core/models/plan.model';
import { PlanService } from '../../core/services/plan.service';
import { planError } from '../../core/errors/plan-error';

@Component({
  selector: 'app-plan-form',
  imports: [ReactiveFormsModule, LucideX],
  templateUrl: './plan-form.html',
  styleUrl: '../../shared/styles/admin-form.css',
})
export class PlanForm {
  private readonly service = inject(PlanService);
  private readonly builder = inject(FormBuilder);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly editing = signal<Plan | null>(null);
  readonly saved = output<Plan>();
  readonly saving = signal(false);
  readonly error = signal('');
  readonly limits = [
    { key: 'maxTables', label: 'Máximo de mesas' },
    { key: 'maxUsers', label: 'Máximo de usuarios' },
    { key: 'maxProducts', label: 'Máximo de productos' },
  ] as const;
  private readonly positiveInteger = [
    Validators.required,
    Validators.min(1),
    Validators.max(2147483647),
    Validators.pattern(/^[1-9]\d*$/),
  ];
  readonly form = this.builder.group({
    name: this.builder.nonNullable.control('', [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(100),
    ]),
    maxTables: this.builder.control<number | null>(null, this.positiveInteger),
    maxUsers: this.builder.control<number | null>(null, this.positiveInteger),
    maxProducts: this.builder.control<number | null>(null, this.positiveInteger),
    active: this.builder.nonNullable.control(true),
  });

  open(plan: Plan | null = null) {
    if (this.saving()) return;
    this.editing.set(plan);
    this.error.set('');
    this.form.reset({
      name: plan?.name ?? '',
      maxTables: plan?.maxTables ?? null,
      maxUsers: plan?.maxUsers ?? null,
      maxProducts: plan?.maxProducts ?? null,
      active: plan?.active ?? true,
    });
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
    const value = this.form.getRawValue();
    if (value.maxTables === null || value.maxUsers === null || value.maxProducts === null) return;
    const request: PlanRequest = {
      ...value,
      name: value.name.trim(),
      maxTables: value.maxTables,
      maxUsers: value.maxUsers,
      maxProducts: value.maxProducts,
    };
    this.saving.set(true);
    this.error.set('');
    this.form.disable();
    const editing = this.editing();
    const operation = editing
      ? this.service.update(editing.id, request)
      : this.service.create(request);
    operation
      .pipe(
        finalize(() => {
          this.saving.set(false);
          this.form.enable();
        }),
      )
      .subscribe({
        next: (plan) => {
          this.saved.emit(plan);
          this.dialog().nativeElement.close();
        },
        error: (error: unknown) => this.error.set(planError(error)),
      });
  }
}
