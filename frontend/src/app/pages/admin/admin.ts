import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import {
  LucidePlus,
  LucideSearch,
  LucideStore,
  LucideCheck,
  LucideShieldCheck,
} from '@lucide/angular';
import { keycloak } from '../../auth/keycloak';
import { BusinessCreateForm } from '../../components/business-create-form/business-create-form';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { Business } from '../../core/models/business.model';
import { BusinessService } from '../../core/services/business.service';

@Component({
  selector: 'app-admin',
  imports: [
    DatePipe,
    Header,
    Sidebar,
    BusinessCreateForm,
    LucidePlus,
    LucideSearch,
    LucideStore,
    LucideCheck,
    LucideShieldCheck,
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin {
  private readonly businessService = inject(BusinessService);
  readonly createForm = viewChild.required(BusinessCreateForm);
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? 'Administrador';
  readonly businesses = this.businessService.createdBusinesses;
  readonly search = signal('');
  readonly status = signal<Business['status'] | ''>('');
  readonly success = signal('');
  readonly filteredBusinesses = computed(() => {
    const query = this.search().trim().toLocaleLowerCase('es');
    return this.businesses().filter(
      (business) =>
        business.name.toLocaleLowerCase('es').includes(query) &&
        (!this.status() || business.status === this.status()),
    );
  });

  businessCreated(business: Business) {
    this.search.set('');
    this.status.set('');
    this.success.set(`${business.name} se creó correctamente. Su dueño ya está vinculado.`);
  }

  clearFilters() {
    this.search.set('');
    this.status.set('');
  }
}
