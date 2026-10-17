import { HttpErrorResponse } from '@angular/common/http';

export function businessCreationError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'No se pudo crear el negocio. Intenta nuevamente.';
  }
  const messages: Record<string, string> = {
    'Identity.UserNotFound': 'No encontramos al dueño. Revisa su ID en Keycloak.',
    'Identity.UserDisabled': 'El usuario del dueño está deshabilitado en Keycloak.',
    'Identity.OwnerRoleRequired': 'El usuario debe tener el rol owner en Keycloak.',
    'Membership.UserAlreadyAssigned': 'Este usuario ya pertenece a un negocio. Elige otro dueño.',
    'Membership.OwnerAlreadyAssigned': 'Este negocio ya tiene un dueño asignado.',
    'Data.Conflict': 'Ya existe un registro con esos datos. Revisa la información.',
  };
  const code = error.error?.code;
  if (typeof code === 'string' && Object.hasOwn(messages, code)) return messages[code];
  switch (error.status) {
    case 0:
      return 'No pudimos conectar. Revisa tu conexión antes de volver a intentarlo.';
    case 400:
      return 'Revisa el nombre del negocio y el ID del dueño.';
    case 401:
      return 'Tu sesión expiró. Recarga la página e inicia sesión nuevamente.';
    case 403:
      return 'Tu cuenta no tiene permiso para crear negocios.';
    case 503:
      return 'No pudimos verificar al dueño. Intenta nuevamente en unos momentos.';
    default:
      return 'No se pudo confirmar la creación. Verifica el resultado antes de volver a intentarlo.';
  }
}
