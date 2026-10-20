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
import { Observable, Subscription, finalize } from 'rxjs';
import { AvailableWaiter, TeamMember } from '../../core/models/team-member.model';
import { Page } from '../../core/models/page.model';
import { TeamService } from '../../core/services/team.service';
import { teamError } from '../../core/errors/team-error';

@Component({
  selector: 'app-team-member-dialog',
  imports: [ReactiveFormsModule, LucideX],
  templateUrl: './team-member-dialog.html',
  styleUrls: ['../../shared/styles/admin-form.css', './team-member-dialog.css'],
})
export class TeamMemberDialog {
  private readonly service = inject(TeamService);
  private readonly builder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private availableRequest?: Subscription;
  private appliedSearch = '';
  readonly member = signal<TeamMember | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly search = signal('');
  readonly available = signal<Page<AvailableWaiter> | null>(null);
  readonly availableLoading = signal(false);
  readonly availableError = signal('');
  readonly changed = output<string>();
  readonly form = this.builder.nonNullable.group({
    keycloakUserId: ['', Validators.required],
  });

  open(member: TeamMember | null = null) {
    if (this.saving() || member?.role === 'OWNER') return;
    this.member.set(member);
    this.availableRequest?.unsubscribe();
    this.form.reset({ keycloakUserId: '' });
    this.search.set('');
    this.appliedSearch = '';
    this.available.set(null);
    this.availableLoading.set(false);
    this.availableError.set('');
    this.error.set('');
    this.dialog().nativeElement.showModal();
    if (!member) this.loadAvailable();
  }

  close() {
    if (this.saving()) return;
    this.availableRequest?.unsubscribe();
    this.dialog().nativeElement.close();
  }

  cancel(event: Event) {
    if (this.saving()) event.preventDefault();
    else this.close();
  }

  searchAvailable(event?: Event) {
    event?.preventDefault();
    if (this.saving()) return;
    this.appliedSearch = this.search().trim();
    this.loadAvailable();
  }

  loadAvailable(page = 0) {
    if (this.saving() || this.member() || page < 0) return;
    this.availableRequest?.unsubscribe();
    this.form.controls.keycloakUserId.setValue('');
    this.available.set(null);
    this.availableLoading.set(true);
    this.availableError.set('');
    this.availableRequest = this.service
      .availableWaiters(this.appliedSearch, page)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (available) => {
          if (available.totalPages > 0 && available.page >= available.totalPages) {
            this.loadAvailable(available.totalPages - 1);
            return;
          }
          this.available.set(available);
          this.availableLoading.set(false);
        },
        error: (error: unknown) => {
          this.availableError.set(teamError(error));
          this.availableLoading.set(false);
        },
      });
  }

  submit() {
    if (this.saving()) return;
    const member = this.member();
    this.form.markAllAsTouched();
    if (!member && (this.form.invalid || this.availableLoading())) return;
    const keycloakUserId = this.form.getRawValue().keycloakUserId;
    const selected = this.available()?.items.find((waiter) => waiter.id === keycloakUserId);
    if (!member && !selected) return;
    const operation: Observable<TeamMember | void> = member
      ? this.service.remove(member.id)
      : this.service.add({ keycloakUserId });
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
        next: (added) => {
          this.changed.emit(
            member
              ? 'Mozo desvinculado. Sus pedidos se conservaron.'
              : `${added?.username ?? selected?.username} ya forma parte del equipo.`,
          );
          this.dialog().nativeElement.close();
        },
        error: (error: unknown) => this.error.set(teamError(error)),
      });
  }
}
