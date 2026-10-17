import { inject } from '@angular/core';
import { Router, Routes } from '@angular/router';
import { adminGuard } from './auth/admin.guard';
import { keycloak } from './auth/keycloak';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [
      () =>
        keycloak.authenticated && keycloak.hasRealmRole('admin')
          ? inject(Router).parseUrl('/admin')
          : true,
    ],
    loadComponent: () => import('./pages/home/home').then((module) => module.Home),
    title: 'Goshop · Panel de servicio',
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin/admin').then((module) => module.Admin),
    title: 'Goshop · Administración',
  },
  {
    path: '**',
    loadComponent: () => import('./pages/notfound/notfound').then((module) => module.Notfound),
    title: 'Goshop · Página no encontrada',
  },
];
