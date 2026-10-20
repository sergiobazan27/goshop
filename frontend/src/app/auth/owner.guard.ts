import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { keycloak } from './keycloak';

export const ownerGuard: CanActivateFn = () =>
  keycloak.authenticated && keycloak.hasRealmRole('owner') ? true : inject(Router).parseUrl('/');
