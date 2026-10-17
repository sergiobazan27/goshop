import { HttpInterceptorFn } from '@angular/common/http';
import { defer, switchMap } from 'rxjs';
import { keycloak } from './keycloak';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const url = new URL(request.url, window.location.origin);
  const apiUrl = new URL(environment.apiUrl, window.location.origin);
  const apiPath = apiUrl.pathname.replace(/\/+$/, '');

  const isOurApi =
    url.origin === apiUrl.origin &&
    (url.pathname === apiPath || url.pathname.startsWith(`${apiPath}/`));

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
