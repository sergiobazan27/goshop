import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { keycloak } from '../../auth/keycloak';
import {
  LucideArmchair,
  LucideLayoutDashboard,
  LucidePackage,
  LucideReceiptText,
  LucideStore,
  LucideUsers,
} from '@lucide/angular';

@Component({
  imports: [
    RouterLink,
    RouterLinkActive,
    LucideArmchair,
    LucideLayoutDashboard,
    LucidePackage,
    LucideReceiptText,
    LucideStore,
    LucideUsers,
  ],
  selector: 'app-sidebar',
  styleUrl: './sidebar.css',
  templateUrl: './sidebar.html',
})
export class Sidebar {
  readonly mode = input<'business' | 'admin'>('business');
  readonly username = input(keycloak.tokenParsed?.['preferred_username'] ?? 'Mi cuenta');
  readonly businessName = input('Mi negocio');
  readonly canManageTables = keycloak.hasRealmRole('owner');
  readonly canViewCatalog = this.canManageTables || keycloak.hasRealmRole('waiter');
  readonly roleLabel = input(
    keycloak.hasRealmRole('owner')
      ? 'Dueño del negocio'
      : keycloak.hasRealmRole('waiter')
        ? 'Mozo'
        : 'Miembro',
  );
}
