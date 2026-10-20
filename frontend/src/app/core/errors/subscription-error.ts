import { HttpErrorResponse } from '@angular/common/http';

export function subscriptionError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const messages: Record<string, string> = {
      'Subscription.AlreadyAssigned':
        'El negocio ya tiene una suscripción. Actualiza la información para renovarla o cambiar el plan.',
      'Subscription.NotFound': 'El negocio no tiene una suscripción. Actualiza la información.',
      'Subscription.InvalidDates':
        'El vencimiento debe ser posterior al inicio y a la hora actual.',
      'Subscription.StartFixed': 'La suscripción aún no vence y debe conservar su inicio actual.',
      'Subscription.ExtensionRequired': 'La renovación debe extender el vencimiento actual.',
      'Subscription.InvalidStart':
        'El nuevo inicio debe ser igual o posterior al vencimiento previo.',
      'Plan.Inactive': 'El plan ya no está activo. Elige otro plan disponible.',
      'Plan.NotFound': 'El plan ya no está disponible. Actualiza las opciones.',
    };
    const code = error.error?.code;
    if (typeof code === 'string' && Object.hasOwn(messages, code)) return messages[code];
    if (error.status === 400) return 'Revisa el plan y las fechas de vigencia.';
    if (error.status === 401) return 'Tu sesión expiró. Recarga la página para iniciar sesión.';
    if (error.status === 403) return 'Tu cuenta no tiene permiso para administrar suscripciones.';
    if (error.status === 404) return 'No se encontró el negocio o la suscripción.';
  }
  return 'No pudimos completar la operación. Actualiza la información antes de volver a intentarlo.';
}
