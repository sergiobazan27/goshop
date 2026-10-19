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
Las suscripciones, aplicación de límites, vencimiento, cobros y auditoría siguen pendientes.

Verificación: `npm run build` y `npm test -- --watch=false`. Las pruebas del formulario
simulan HTTP y no crean negocios en el entorno real.

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
