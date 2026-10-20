import { HttpErrorResponse } from '@angular/common/http';

export function overviewError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.error?.code) {
      case 'Membership.Required':
        return 'Tu cuenta no está vinculada a un negocio. Solicita al dueño que te agregue al equipo.';
      case 'Business.Inactive':
        return 'El negocio está suspendido. Contacta al administrador de Goshop.';
      case 'Subscription.Required':
        return 'El negocio necesita una suscripción. Contacta al administrador de Goshop.';
      case 'Subscription.NotStarted':
        return 'La suscripción todavía no ha comenzado.';
      case 'Subscription.Expired':
        return 'La suscripción venció. Solicita su renovación al administrador de Goshop.';
    }
    switch (error.status) {
      case 0:
        return 'No pudimos conectar con el servidor. Revisa tu conexión y vuelve a intentar.';
      case 401:
        return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
      case 403:
        return 'Tu cuenta no tiene acceso al resumen del negocio.';
    }
  }
  return 'No pudimos cargar el resumen. Vuelve a intentarlo.';
}
