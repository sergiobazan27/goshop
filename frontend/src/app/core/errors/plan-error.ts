import { HttpErrorResponse } from '@angular/common/http';

export function planError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.status) {
      case 400:
        return 'Revisa el nombre y los límites. Usa números enteros mayores que cero.';
      case 401:
        return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
      case 403:
        return 'Tu cuenta no tiene permiso para administrar planes.';
      case 404:
        return 'No se encontró el plan. Actualiza el listado.';
    }
  }
  return 'No pudimos completar la operación. Revisa el resultado antes de volver a intentarlo.';
}
