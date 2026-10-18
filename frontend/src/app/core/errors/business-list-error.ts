import { HttpErrorResponse } from '@angular/common/http';

export function businessListError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401)
      return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
    if (error.status === 403) return 'Tu cuenta no tiene permiso para consultar los negocios.';
    if (error.status === 400) return 'Revisa los filtros de búsqueda e inténtalo nuevamente.';
  }
  return 'No pudimos cargar los negocios. Revisa tu conexión e inténtalo nuevamente.';
}
