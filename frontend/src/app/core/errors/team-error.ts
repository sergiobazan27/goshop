import { HttpErrorResponse } from '@angular/common/http';

export function teamError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.error?.code) {
      case 'Identity.UserNotFound':
        return 'La cuenta ya no está disponible. Actualiza la lista de mozos.';
      case 'Identity.UserDisabled':
        return 'La cuenta está deshabilitada en Keycloak.';
      case 'Identity.WaiterRoleRequired':
        return 'La cuenta debe tener el rol waiter en Keycloak.';
      case 'Identity.PrivilegedUser':
        return 'Las cuentas owner o admin no se pueden vincular como mozos.';
      case 'Membership.UserAlreadyAssigned':
        return 'Esta cuenta ya pertenece a un negocio. Actualiza la lista de mozos.';
      case 'Membership.LimitReached':
        return 'Alcanzaste el límite de usuarios, incluido el dueño. Desvincula un mozo o solicita ampliar tu plan.';
      case 'Membership.OwnerProtected':
        return 'El dueño no se puede desvincular desde Equipo.';
      case 'Membership.OwnerRequired':
        return 'Solo el dueño del negocio puede gestionar el equipo.';
      case 'Membership.Required':
        return 'Tu cuenta no pertenece a un negocio.';
      case 'Membership.Changed':
        return 'Tu membresía cambió. Vuelve a iniciar sesión.';
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
      case 400:
        return 'Revisa la búsqueda y selecciona un mozo disponible.';
      case 401:
        return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
      case 403:
        return 'Tu cuenta no tiene permiso para gestionar el equipo.';
      case 404:
        return 'No se encontró el integrante. Actualiza el listado.';
      case 503:
        return 'No pudimos verificar la cuenta en Keycloak. Intenta nuevamente en unos momentos.';
    }
  }
  return 'No pudimos completar la operación. Actualiza el equipo antes de volver a intentarlo.';
}
