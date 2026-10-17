import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { keycloak } from './keycloak';

export const adminGuard: CanActivateFn = () =>
  keycloak.authenticated && keycloak.hasRealmRole('admin') ? true : inject(Router).parseUrl('/');
