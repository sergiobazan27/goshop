import { HttpErrorResponse } from '@angular/common/http';

export function orderError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.error?.code) {
      case 'Order.TableOccupied':
        return 'La mesa ya tiene un pedido abierto. Actualiza las mesas y selecciona Ver pedido.';
      case 'Order.Changed':
        return 'Otro usuario modificó el pedido. Actualízalo y revisa sus cantidades antes de guardar nuevamente.';
      case 'Order.NotOpen':
        return 'El pedido ya no está abierto. Actualiza las mesas.';
      case 'Order.ItemNotFound':
        return 'El producto ya no está en el pedido. Actualiza el pedido.';
      case 'Product.Unavailable':
        return 'El producto ya no está disponible. Actualiza los productos para elegir otro.';
      case 'Product.NotFound':
        return 'No se encontró el producto en tu negocio. Actualiza los productos.';
      case 'Table.NotFound':
        return 'La mesa ya no está disponible. Actualiza el listado de mesas.';
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
        return 'Revisa la mesa y las cantidades. Usa números enteros de 1 a 999, o cero para quitar un producto.';
      case 401:
        return 'Tu sesión expiró. Recarga la página para iniciar sesión nuevamente.';
      case 403:
        return 'Tu cuenta no tiene permiso para gestionar pedidos.';
      case 404:
        return 'No se encontró el pedido. Actualiza las mesas.';
    }
  }
  return 'No pudimos completar la operación. Actualiza el pedido para comprobar el resultado antes de intentarlo nuevamente.';
}
