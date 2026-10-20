import { inject } from '@angular/core';
import { Router, Routes } from '@angular/router';
import { adminGuard } from './auth/admin.guard';
import { ownerGuard } from './auth/owner.guard';
import { catalogGuard } from './auth/catalog.guard';
import { ordersGuard } from './auth/orders.guard';
import { keycloak } from './auth/keycloak';

export const routes: Routes = [
  {
    path: 'team',
    canActivate: [ownerGuard],
    loadComponent: () => import('./pages/team/team').then((module) => module.Team),
    title: 'Goshop · Equipo',
  },
  {
    path: 'orders',
    canActivate: [ordersGuard],
    loadComponent: () => import('./pages/orders/orders').then((module) => module.Orders),
    title: 'Goshop · Pedidos',
  },
  {
    path: 'products',
    canActivate: [catalogGuard],
    loadComponent: () => import('./pages/products/products').then((module) => module.Products),
    title: 'Goshop · Productos',
  },
  {
    path: 'tables',
    canActivate: [ownerGuard],
    loadComponent: () => import('./pages/tables/tables').then((module) => module.Tables),
    title: 'Goshop · Mesas',
  },
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
    path: 'admin/plans',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/plans/plans').then((module) => module.Plans),
    title: 'Goshop · Planes',
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
