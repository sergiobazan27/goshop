import { DatePipe } from '@angular/common';
import { LucideX } from '@lucide/angular';
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
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, Subscription as RxSubscription, finalize } from 'rxjs';
import { Business } from '../../core/models/business.model';
import { Plan } from '../../core/models/plan.model';
import { Page } from '../../core/models/page.model';
import {
  Subscription,
  SubscriptionHistoryEntry,
  SubscriptionMode,
} from '../../core/models/subscription.model';
import { PlanService } from '../../core/services/plan.service';
import { SubscriptionService } from '../../core/services/subscription.service';
import { subscriptionError } from '../../core/errors/subscription-error';

@Component({
  selector: 'app-subscription-dialog',
  imports: [DatePipe, ReactiveFormsModule, LucideX],
  templateUrl: './subscription-dialog.html',
  styleUrl: '../../shared/styles/admin-form.css',
})
export class SubscriptionDialog {
  private readonly service = inject(SubscriptionService);
  private readonly planService = inject(PlanService);
  private readonly builder = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readRequest?: RxSubscription;
  private plansRequest?: RxSubscription;
  private historyRequest?: RxSubscription;
  readonly business = signal<Business | null>(null);
  readonly current = signal<Subscription | null>(null);
  readonly history = signal<Page<SubscriptionHistoryEntry> | null>(null);
  readonly plans = signal<Page<Plan> | null>(null);
  readonly mode = signal<SubscriptionMode | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly plansLoading = signal(false);
  readonly historyLoading = signal(false);
  readonly error = signal('');
  readonly formError = signal('');
  readonly plansError = signal('');
  readonly historyError = signal('');
  readonly success = signal('');
  readonly planSearch = signal('');
  readonly saved = output<Subscription>();
  readonly zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  readonly statuses = { ACTIVE: 'Vigente', NOT_STARTED: 'Programada', EXPIRED: 'Vencida' };
  readonly actions = {
    ASSIGNED: 'Asignación',
    RENEWED: 'Renovación',
    PLAN_CHANGED: 'Cambio de plan',
  };
  readonly form = this.builder.group({
    planId: ['', Validators.required],
    startsAt: [''],
    expiresAt: [''],
  });

  open(business: Business) {
    if (this.saving()) return;
    this.business.set(business);
    this.current.set(null);
    this.history.set(null);
    this.mode.set(null);
    this.success.set('');
    this.formError.set('');
    this.dialog().nativeElement.showModal();
    this.load();
  }

  close() {
    if (this.saving()) return;
    this.readRequest?.unsubscribe();
    this.plansRequest?.unsubscribe();
    this.historyRequest?.unsubscribe();
    this.dialog().nativeElement.close();
  }

  cancel(event: Event) {
    if (this.saving()) event.preventDefault();
    else this.close();
  }

  load() {
    const business = this.business();
    if (!business || this.saving()) return;
    this.readRequest?.unsubscribe();
    this.historyRequest?.unsubscribe();
    this.plansRequest?.unsubscribe();
    this.mode.set(null);
    this.historyLoading.set(false);
    this.plansLoading.set(false);
    this.loading.set(true);
    this.error.set('');
    this.historyError.set('');
    this.readRequest = this.service
      .details(business.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (details) => {
          this.current.set(details.current);
          this.history.set(details.history);
          this.loading.set(false);
          if (!details.current) this.choose('assign');
        },
        error: (error: unknown) => {
          this.error.set(subscriptionError(error));
          this.loading.set(false);
        },
      });
  }

  choose(mode: SubscriptionMode) {
    if (this.saving() || this.loading()) return;
    this.mode.set(mode);
    this.formError.set('');
    this.success.set('');
    const now = new Date();
    const localNow = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);
    this.form.reset({ planId: '', startsAt: mode === 'assign' ? localNow : '', expiresAt: '' });
    this.form.controls.startsAt.setValidators(mode === 'assign' ? [Validators.required] : []);
    this.form.controls.expiresAt.setValidators(mode === 'change' ? [] : [Validators.required]);
    this.form.controls.startsAt.updateValueAndValidity();
    this.form.controls.expiresAt.updateValueAndValidity();
    this.planSearch.set('');
    this.loadPlans();
  }

  loadPlans(page = 0) {
    if (this.saving() || page < 0) return;
    this.form.controls.planId.setValue('');
    this.plansRequest?.unsubscribe();
    this.plans.set(null);
    this.plansLoading.set(true);
    this.plansError.set('');
    this.plansRequest = this.planService
      .list({ search: this.planSearch(), active: 'true', page, size: 25 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (plans) => {
          this.plans.set(plans);
          this.plansLoading.set(false);
        },
        error: (error: unknown) => {
          this.plans.set(null);
          this.plansError.set(subscriptionError(error));
          this.plansLoading.set(false);
        },
      });
  }

  selectedPlan() {
    return this.plans()?.items.find((plan) => plan.id === this.form.controls.planId.value);
  }

  loadHistory(page: number) {
    const business = this.business();
    if (!business || this.saving()) return;
    this.historyRequest?.unsubscribe();
    this.historyLoading.set(true);
    this.historyError.set('');
    this.historyRequest = this.service
      .history(business.id, page)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (history) => {
          this.history.set(history);
          this.historyLoading.set(false);
        },
        error: (error: unknown) => {
          this.historyError.set(subscriptionError(error));
          this.historyLoading.set(false);
        },
      });
  }

  submit() {
    const business = this.business(),
      mode = this.mode();
    if (!business || !mode || this.saving() || this.plansLoading()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const values = this.form.getRawValue();
    const startDate = values.startsAt ? new Date(values.startsAt) : null;
    const endDate = values.expiresAt ? new Date(values.expiresAt) : null;
    if (
      (startDate && !Number.isFinite(startDate.getTime())) ||
      (endDate && !Number.isFinite(endDate.getTime()))
    ) {
      this.formError.set('Revisa las fechas de vigencia.');
      return;
    }
    if (startDate && endDate && endDate <= startDate) {
      this.formError.set('El vencimiento debe ser posterior al inicio.');
      return;
    }
    const start = startDate?.toISOString(),
      end = endDate?.toISOString();
    let operation: Observable<Subscription>;
    if (mode === 'assign') {
      if (!start || !end) return;
      operation = this.service.assign(business.id, {
        planId: values.planId,
        startsAt: start,
        expiresAt: end,
      });
    } else if (mode === 'renew') {
      if (!end) return;
      operation = this.service.renew(business.id, {
        planId: values.planId,
        ...(start ? { startsAt: start } : {}),
        expiresAt: end,
      });
    } else {
      operation = this.service.changePlan(business.id, values.planId);
    }
    this.saving.set(true);
    this.form.disable();
    this.formError.set('');
    operation
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving.set(false);
          this.form.enable();
        }),
      )
      .subscribe({
        next: (subscription) => {
          this.current.set(subscription);
          this.saved.emit(subscription);
          this.success.set(
            'Suscripción guardada. La suspensión del negocio se administra por separado.',
          );
          this.mode.set(null);
          this.saving.set(false);
          this.load();
        },
        error: (error: unknown) => this.formError.set(subscriptionError(error)),
      });
  }
}
