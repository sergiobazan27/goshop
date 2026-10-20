import { HttpErrorResponse } from '@angular/common/http';

export function productError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.error?.code) {
      case 'Product.LimitReached':
        return 'Alcanzaste el límite de productos. Solicita ampliar tu plan; los no disponibles también cuentan.';
      case 'Product.DuplicateName':
        return 'Ya existe un producto con ese nombre. Elige otro.';
      case 'Business.Inactive':
        return 'El negocio está suspendido. Contacta al administrador de Goshop.';
      case 'Subscription.Required':
        return 'El negocio necesita una suscripción. Contacta al administrador de Goshop.';
      case 'Subscription.NotStarted':
        return 'La suscripción todavía no ha comenzado.';
      case 'Subscription.Expired':
        return 'La suscripción venció. Solicita su renovación al administrador de Goshop.';
      case 'Membership.Required':
        return 'Tu cuenta todavía no pertenece a un negocio.';
    }
    switch (error.status) {
      case 400:
        return 'Revisa el nombre, la descripción y el precio. El precio debe ser positivo, con hasta dos decimales.';
      case 401:
        return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
      case 403:
        return 'Tu cuenta no tiene permiso para realizar esta operación en el catálogo.';
      case 404:
        return 'No se encontró el producto. Actualiza el listado.';
    }
  }
  return 'No pudimos completar la operación. Actualiza el listado antes de volver a intentarlo.';
}
