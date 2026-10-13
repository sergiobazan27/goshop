import { HttpInterceptorFn } from '@angular/common/http';
import { defer, switchMap } from 'rxjs';
import { keycloak } from './keycloak';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const url = new URL(request.url, window.location.origin);

  const isOurApi =
    url.origin === 'http://localhost:8080' &&
    (url.pathname === '/api' || url.pathname.startsWith('/api/'));

  if (!isOurApi) {
    return next(request);
  }

  return defer(async () => {
    await keycloak.updateToken(30);

    const token = keycloak.token;
    if (!token) {
      throw new Error('No hay una sesion autenticada.');
    }

    return token;
  }).pipe(
    switchMap((token) =>
      next(
        request.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        }),
      ),
    ),
  );
};
