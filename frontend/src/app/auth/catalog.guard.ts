import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { keycloak } from './keycloak';

export const catalogGuard: CanActivateFn = () =>
  keycloak.authenticated && (keycloak.hasRealmRole('owner') || keycloak.hasRealmRole('waiter'))
    ? true
    : inject(Router).parseUrl('/');
