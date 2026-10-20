# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.2.1.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Panel de administración

El usuario con rol de realm `admin` entra automáticamente en `/admin`. El guard de Angular
controla la navegación; el backend continúa validando los permisos de cada petición.
Los usuarios operativos mantienen el panel de servicio en `/`.

El formulario **Nuevo negocio** utiliza `POST /admin/businesses` sobre la URL base de la API
mediante el interceptor de autenticación existente. Requiere un dueño habilitado en Keycloak,
con rol `owner`, que todavía no pertenezca a otro negocio. Se muestran errores de validación,
permisos, pertenencia y disponibilidad sin descartar lo escrito.

La tabla consulta `GET /admin/businesses` al entrar y después de crear un negocio. El listado
proviene de PostgreSQL y sigue disponible al recargar. El formulario de búsqueda aplica el
nombre al pulsar **Buscar** o Enter; el selector aplica el estado inmediatamente. Ambas
acciones vuelven a la primera página. Las páginas contienen 10 negocios, con controles
**Anterior** y **Siguiente** y el total de coincidencias del servidor.

`BusinessService.list()` envía `search`, `status`, `page` (base cero) y `size` usando `HttpParams`.
Los contratos están en `core/models/business.model.ts` y `core/models/page.model.ts`.
El panel muestra estados de carga, resultados vacíos y errores con reintento; cancela las
consultas anteriores al cambiar los filtros o la página. No utiliza un listado temporal local.
Cada fila permite **Suspender** o **Reactivar** mediante un diálogo que explica el cambio.
Cancelar o cerrar el diálogo no envía peticiones. Al confirmar, `BusinessService.changeStatus()`
envía `PUT /admin/businesses/{id}/status` con el estado deseado. Se bloquean los envíos duplicados
y el cierre mientras se guarda. Los errores mantienen abierto el diálogo para corregir o reintentar.
Después del cambio se actualiza el listado conservando filtros y página; si deja de existir
esa página, se consulta la última disponible. Los datos del negocio no se eliminan.
El catálogo de planes está disponible en **Planes** (`/admin/plans`), protegido por el mismo
guard de administrador. Permite buscar y paginar, crear y editar el nombre, límites y estado.
El formulario compartido de creación/edición valida enteros positivos y permite desactivar o
reactivar el plan mediante **Plan activo**. Los cambios se guardan con `PlanService`, usando
el environment; los contratos están en `core/models/plan.model.ts`.

Los formularios y las pantallas administrativas reutilizan estilos locales en `shared/styles/`.
En **Negocios → Suscripción** se puede asignar, renovar y cambiar el plan, y consultar el
historial paginado. El selector consulta planes activos con búsqueda y paginación. Los contratos
están en `core/models/subscription.model.ts` y las peticiones en `SubscriptionService`, usando
el environment. Las fechas del formulario usan la zona del navegador y se envían como UTC.

Renovar una suscripción sin vencer conserva el inicio y exige ampliar el vencimiento.
Si venció, dejar el inicio vacío usa la hora del servidor. Cambiar de plan conserva las fechas;
ninguna de estas acciones reactiva un negocio suspendido. El historial conserva las condiciones
y el administrador que realizó cada cambio. El dashboard muestra avisos de suspensión, ausencia
de suscripción, inicio futuro o vencimiento; el backend valida el acceso en cada operación.
Los cobros y la auditoría general siguen pendientes.
Para esta funcionalidad se verificó el build sin agregar ni ejecutar pruebas.

Verificación: `npm run build` y `npm test -- --watch=false`. Las pruebas del formulario
simulan HTTP y no crean negocios en el entorno real.

## Configuración de mesas

El dueño accede desde **Mesas** (`/tables`). El listado paginado permite crear, editar y eliminar
mesas, con confirmación antes de eliminar. El formulario reutilizable valida nombre y capacidad,
bloquea envíos duplicados y conserva los datos ante errores. Las peticiones usan
`DiningTableService` y el environment; sus interfaces están en `core/models/dining-table.model.ts`.

La pantalla muestra el uso frente al límite contratado y deshabilita **Nueva mesa** al alcanzarlo.
El servidor comprueba el límite y la pertenencia al negocio en cada operación. Después de un
cambio se recarga el listado y el resumen del plan; si la última página desaparece, se corrige
la paginación. Un plan menor conserva las mesas y permite editarlas o eliminarlas.
Esta pantalla configura mesas; Resumen muestra su ocupación real.
No se añadieron ni ejecutaron tests para esta funcionalidad.

## Catálogo de productos

En **Productos** (`/products`), el dueño crea y edita nombre, descripción opcional, precio y
disponibilidad. El mozo consulta el mismo listado paginado sin controles de edición.
La ruta usa `catalogGuard`; los permisos reales se comprueban también en el backend.

`ProductForm` reutiliza los estilos de formularios, valida precios positivos con hasta dos
decimales y bloquea envíos duplicados. Los contratos están en `core/models/product.model.ts`;
`ProductService` concentra las peticiones usando el environment. La pantalla muestra el consumo
de `maxProducts` y deshabilita nuevas altas al alcanzar el límite. Los productos no disponibles
siguen contando. Un plan menor conserva el catálogo y permite editarlo. Guardar recarga la página
actual y el perfil; **Actualizar** permite consultar cambios de disponibilidad del dueño.

El precio se muestra con dos decimales sin asumir una moneda. No hay eliminación, categorías,
imágenes ni promociones en esta etapa. El catálogo se usa para agregar productos a pedidos.
Resumen consulta el número real de productos disponibles. No se añadieron ni ejecutaron tests para esta funcionalidad.

## Pedidos por mesa

**Pedidos** (`/orders`) está disponible para `owner` y `waiter`. El botón **Nuevo pedido** del
dashboard también lleva a esta pantalla. Las tarjetas consultan mesas reales y muestran si están
libres u ocupadas, las unidades y el total. **Abrir pedido** crea uno vacío; **Ver pedido** consulta
el existente. La pantalla permite paginar mesas y actualizar su estado manualmente.

Al elegir una mesa se muestra una vista dedicada, con botón para volver al salón y su historial.
`OrderEditor` conserva el diseño de la demo: catálogo paginado en tarjetas a la izquierda y resumen
con cabecera morada a la derecha, alineado con el buscador y fijo al desplazarse en escritorio.
La fuente local Instrument Sans es la misma de la demo. El buscador filtra nombre y descripción
sobre todo el catálogo, con paginación del servidor y cancelación de consultas anteriores.
Las pestañas **Todos**, **Disponibles** y **No disponibles** filtran la disponibilidad real;
las categorías aún no forman parte del modelo. El botón **+** agrega una unidad;
los controles **− / +** del resumen guardan cada cambio y quitar la última unidad elimina la línea.
En móvil, el resumen queda debajo de la carta y tiene un enlace de acceso directo. No se muestran
notas ni envío a cocina porque esas operaciones todavía no están implementadas. Los precios,
subtotales y el total siempre proceden del servidor. Se conserva el precio aplicado al cambiar
cantidades, y los productos no disponibles no se pueden agregar ni incrementar. El catálogo
incluye una opción para actualizar la disponibilidad.

`OrderService` concentra las peticiones con el environment; los contratos están en
`core/models/order.model.ts`. El guard limita la ruta a los dos roles y el backend aplica los
permisos. Los envíos duplicados y el cambio de mesa durante el guardado se bloquean. Cada cambio
envía la revisión del pedido: ante conflicto, **Actualizar pedido** carga las cantidades actuales
antes de intentar nuevamente. **Actualizar mesas** refresca ocupación y totales de las tarjetas.

**Finalizar atención** abre una confirmación con la mesa y el total guardado. Cancelar no envía
peticiones; confirmar bloquea duplicados y el cierre del diálogo mientras se guarda. Un conflicto
de revisión conserva el diálogo y permite volver al pedido para actualizarlo. El dueño y el mozo
pueden cerrar, incluso si el pedido está vacío. El cierre libera la mesa, conserva las líneas y
registra responsable y fecha; no registra pagos.

Después del cierre se actualizan tarjetas e historial y el detalle pasa a solo lectura.
**Pedidos cerrados** es un historial paginado con acceso al detalle mediante **Ver detalle**.
`OrderCloseDialog` y `OrderHistory` reutilizan los estilos compartidos; las peticiones continúan
en `OrderService`. El listado y detalle de `/orders` y los componentes de Resumen usan datos reales.
Cocina, cobros y reapertura quedan pendientes. No se añadieron
ni ejecutaron tests.

## Gestión del equipo

El dueño accede desde **Equipo** (`/team`), protegido por `ownerGuard`. El listado incluye al
dueño y los mozos, con rol y fecha de vinculación, y muestra el uso del límite `maxUsers`.
**Agregar mozo** muestra un selector de cuentas existentes y habilitadas en Keycloak,
con rol `waiter`, sin rol `owner`/`admin` y sin membresía en ningún negocio. Cada opción muestra
el nombre y el usuario; **Buscar** filtra por nombre o usuario y los controles permiten paginar.
Cambiar de búsqueda o página limpia la selección; las consultas anteriores se cancelan al
reemplazarlas o cerrar el diálogo. Hay estados de carga, lista vacía, error y reintento.

`TeamService.availableWaiters()` consulta `GET /team/available-waiters` con búsqueda y paginación;
`TeamService.add()` envía solo `{ keycloakUserId }`. **Actualizar lista** permite recuperar
cambios de disponibilidad. El servidor revalida la cuenta y el límite al asignar: si otro negocio
la vinculó mientras el diálogo estaba abierto, se muestra el conflicto y se puede actualizar.

**Desvincular** solicita confirmación y retira el acceso al negocio conservando la cuenta y
los pedidos. No se ofrece esta acción para el dueño. El formulario bloquea envíos duplicados,
mantiene los datos ante errores y actualiza el listado al guardar. La paginación se corrige si
desaparece la última página. Los controles de alta se deshabilitan al alcanzar el límite del
plan, que incluye al dueño; el servidor vuelve a comprobar todas las reglas.

`TeamMemberDialog` reutiliza los estilos de formularios; `TeamService` concentra HTTP con el
environment y los contratos están en `core/models/team-member.model.ts`. No se crean cuentas
ni asignan roles en Keycloak desde esta pantalla. Después de vincular, el mozo puede consultar
`/api/me` y entrar a **Pedidos** (`/orders`) para ver mesas y gestionar sus pedidos.
No se añadieron ni ejecutaron tests para esta funcionalidad.

## Resumen del negocio

La página inicial consulta el perfil y `GET /overview` mediante `OverviewService`, usando el
environment. Sus contratos están en `core/models/overview.model.ts`. Los contadores abarcan todo
el negocio: mesas ocupadas frente a configuradas, pedidos abiertos y productos disponibles.
No representan ventas, pagos ni envíos a cocina.

Los componentes existentes reciben datos mediante inputs: `ServiceSummary`, `TablesOverview`,
`TableCard`, `RecentOrders`, `RecentOrder` y `OrdersTable`. Se muestran hasta 12 mesas, cinco
pedidos en curso y los diez pedidos actualizados más recientemente, con estados abierto/cerrado,
cantidades, precios totales y fechas reales. Cada sección contempla una lista vacía.

**Actualizar** vuelve a consultar perfil y resumen, cancela consultas anteriores y muestra la hora
de actualización. La página incluye carga, error y reintento; conserva los avisos de membresía,
suspensión y suscripción y no presenta cifras de ejemplo ni ceros ficticios ante fallos.
Cabecera, fecha, negocio, usuario y pie ya no usan valores del prototipo.

Los enlaces a pedidos y actividad abren `/orders?orderId=...`, consultan el pedido y enfocan su
detalle, incluido el historial cerrado. Una mesa libre lleva a `/orders?tableId=...` y queda
resaltada: navegar no crea pedidos, hay que pulsar **Abrir pedido**. La carta lleva a `/products`,
con texto de edición para el dueño y consulta para el mozo.
No se añadieron ni ejecutaron tests para esta funcionalidad.

## Configuración y organización

- `src/environments/environment.development.ts`: configuración local. `npm start` utiliza
  la API `http://localhost:8080/api` y Keycloak `http://localhost:8180`.
- `src/environments/environment.ts`: configuración de producción, usada por `npm run build`.
  La API usa `/api` y Keycloak `/auth`, ambos en el mismo origen. Configura estas rutas en
  el proxy de despliegue o reemplaza sus valores por las URLs reales antes de publicar.
- `angular.json`: la configuración `development` reemplaza el archivo de producción por
  `environment.development.ts`. Las pruebas utilizan esta configuración de desarrollo.
- `src/app/core/services/`: servicios HTTP; cada servicio define sus endpoints usando `environment.apiUrl`.
- `src/app/core/models/`: interfaces compartidas de negocios, solicitudes y usuario autenticado.
- `src/app/core/errors/`: mensajes de errores para la interfaz.

Los componentes consumen servicios. El interceptor compara origen y prefijo de ruta con
`environment.apiUrl` antes de adjuntar el token. La configuración de Keycloak también viene
del environment correspondiente. Estos archivos contienen configuración pública, no secretos.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
