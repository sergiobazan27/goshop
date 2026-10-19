import { HttpErrorResponse } from '@angular/common/http';

export function businessStatusError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.status) {
      case 400:
        return 'El estado solicitado no es válido. Actualiza el listado.';
      case 401:
        return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
      case 403:
        return 'Tu cuenta no tiene permiso para cambiar el estado del negocio.';
      case 404:
        return 'El negocio ya no está disponible. Cierra este aviso y actualiza el listado.';
    }
  }
  return 'No pudimos confirmar el cambio. Puedes volver a intentarlo.';
}
