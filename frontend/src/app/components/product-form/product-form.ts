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
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideX } from '@lucide/angular';
import { finalize } from 'rxjs';
import { Product } from '../../core/models/product.model';
import { ProductService } from '../../core/services/product.service';
import { productError } from '../../core/errors/product-error';

@Component({
  selector: 'app-product-form',
  imports: [ReactiveFormsModule, LucideX],
  templateUrl: './product-form.html',
  styleUrl: '../../shared/styles/admin-form.css',
})
export class ProductForm {
  private readonly service = inject(ProductService);
  private readonly builder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly editing = signal<Product | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly saved = output<Product>();
  readonly form = this.builder.group({
    name: this.builder.nonNullable.control('', [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(100),
    ]),
    description: this.builder.nonNullable.control('', [Validators.maxLength(1000)]),
    price: this.builder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.max(9999999999.99),
      Validators.pattern(/^\d+(\.\d{1,2})?$/),
    ]),
    available: this.builder.nonNullable.control(true),
  });

  open(product: Product | null = null) {
    if (this.saving()) return;
    this.editing.set(product);
    this.error.set('');
    this.form.reset({
      name: product?.name ?? '',
      description: product?.description ?? '',
      price: product?.price ?? null,
      available: product?.available ?? true,
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
    const value = this.form.getRawValue();
    if (this.form.invalid || value.price === null) return;
    const request = {
      ...value,
      name: value.name.trim(),
      description: value.description.trim(),
      price: value.price,
    };
    const editing = this.editing();
    const operation = editing
      ? this.service.update(editing.id, request)
      : this.service.create(request);
    this.saving.set(true);
    this.error.set('');
    this.form.disable();
    operation
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving.set(false);
          this.form.enable();
        }),
      )
      .subscribe({
        next: (product) => {
          this.saved.emit(product);
          this.dialog().nativeElement.close();
        },
        error: (error: unknown) => this.error.set(productError(error)),
      });
  }
}
