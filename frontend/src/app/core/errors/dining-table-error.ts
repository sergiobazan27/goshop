import { HttpErrorResponse } from '@angular/common/http';

export function diningTableError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.error?.code) {
      case 'Table.HasOrders':
        return 'No puedes eliminar esta mesa porque tiene pedidos asociados.';
      case 'Table.LimitReached':
        return 'Alcanzaste el límite de mesas de tu plan. Elimina una mesa o solicita ampliar el plan.';
      case 'Table.DuplicateName':
        return 'Ya existe una mesa con ese nombre. Elige otro.';
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
        return 'Revisa el nombre y la capacidad. Usa un número entero mayor que cero.';
      case 401:
        return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
      case 403:
        return 'Tu cuenta no tiene permiso para configurar mesas.';
      case 404:
        return 'No se encontró la mesa. Actualiza el listado.';
    }
  }
  return 'No pudimos completar la operación. Actualiza el listado antes de volver a intentarlo.';
}
