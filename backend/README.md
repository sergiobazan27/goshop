# backend

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/backend-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Business onboarding and exception handling

`POST /api/admin/businesses` requires an authenticated user with the `admin` realm role:

```json
{
  "name": "Café Olivo",
  "ownerKeycloakUserId": "existing-keycloak-user-id"
}
```

The owner must exist, be enabled, and have the `owner` realm role. Keycloak is queried before
the PostgreSQL transaction. The business and its owner membership are committed together;
an unchecked exception leaving the transactional service automatically rolls back both writes.
No manual rollback flags or `rollbackOn` configuration are needed. Subscription assignment
and administrative auditing are not yet implemented by this endpoint.

Services return entities on success and throw custom `RuntimeException` subclasses for business
failures. `BusinessExceptionMapper` translates `ResourceNotFoundException` to `404`,
`BusinessRuleException` to `422`, and `BusinessConflictException` to `409`, using
`{"code":"Membership.UserAlreadyAssigned","message":"El usuario ya pertenece a un negocio."}`.
Success returns `201` with a `BusinessResponse`. Input validation remains `400`, and Quarkus
continues to enforce `401`/`403`. Keycloak transport/technical-account failures return `503`;
unexpected failures are logged server-side and return `500` without technical details.

For development, Hibernate uses schema `update`. The single-owner constraint requires this
PostgreSQL index after the tables exist (no Flyway is used):

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_membership_owner_per_business
    ON membership (business_id) WHERE role = 'OWNER';
```

Review existing data before creating this index. PostgreSQL unique-key violations (SQLSTATE
`23505`), including conflicts detected at commit, return a generic `409` with code `Data.Conflict`
after rollback. Other persistence errors remain technical failures. Services do not catch these
exceptions or inspect constraint names; HTTP translation happens in the API layer.

## Business listing

`GET /api/admin/businesses` requires the `admin` realm role; no business membership is required.

```text
/api/admin/businesses?search=olivo&status=ACTIVE&page=0&size=10
```

- `search`: optional case-insensitive name substring, trimmed, at most 150 characters.
  `%`, `_`, and `!` are literal characters; all values use query parameters.
- `status`: optional `ACTIVE` or `SUSPENDED`.
- `page`: zero-based, defaults to `0`, accepted range `0..1000000`.
- `size`: defaults to `10`, accepted range `1..100`.

Results are ordered by `createdAt` descending, then `id` descending for deterministic ties.
The response contains `items` (business DTOs), `page`, `size`, `total`, and `totalPages`.
An empty result has `items: []`, `total: 0`, and `totalPages: 0`. A page beyond the last
returns no items while retaining the total. Invalid query parameters return `400`;
unauthenticated users receive `401` and non-administrators receive `403`.

From `backend/`, run `./mvnw test` for the HTTP and PostgreSQL listing tests (Docker required),
or `./mvnw package` to run them and package the application. Tests use disposable PostgreSQL
with an isolated profile and simulated security identities. They never query the development
Keycloak or Compose database.

## Suspend and reactivate a business

`PUT /api/admin/businesses/{id}/status` requires the `admin` realm role and accepts:

```json
{"status": "SUSPENDED"}
```

Use `ACTIVE` to reactivate. The operation sets the requested state, so repeating the same
request is safe. It returns `200` with the updated `BusinessResponse`; an unchanged state
does not modify the entity. A missing business returns `404` with code `Business.NotFound`;
a missing or invalid status returns `400`. Non-administrators cannot change business states.

Suspension retains business data and memberships. Operational checks return `403` while
suspended; `/api/me` remains available so the user can see the business state. Reactivation
restores operational access under the existing role and membership checks. No Keycloak user
or session is disabled. Administrative auditing remains pending.

The development CORS configuration allows `PUT` for the Angular origin. HTTP tests cover
status changes, repeated requests, rejected payloads, permissions, and the active → suspended
→ active access cycle with preserved membership. They use simulated OIDC claims and a
disposable PostgreSQL container.

## Plan catalog

The `platform.plan` feature manages the catalog independently of subscriptions. Admin-only routes:

- `GET /api/admin/plans?search=basico&active=true&page=0&size=10`: paginated listing;
  `search` and `active` are optional. Pagination follows the business listing contract.
- `POST /api/admin/plans`: creates a plan and returns `201`.
- `PUT /api/admin/plans/{id}`: replaces its editable details and returns `200`; missing IDs return `404` (`Plan.NotFound`).

Both writes require `name` (nonblank, up to 100 characters), `maxTables`, `maxUsers`,
`maxProducts` (whole numbers from 1 to 2147483647), and `active` (boolean). Decimal limits
are rejected rather than truncated. Responses include these fields, `id`, `createdAt`, and `updatedAt`.
Names are trimmed; searching is case-insensitive and treats wildcard characters literally.
Authentication, role checks, validation, custom exceptions, and automatic transaction rollback
follow the existing application conventions. Managed entity updates use automatic flushing at commit.

Plans can be made inactive through editing; there is no delete route. This step does not assign
subscriptions, enforce limits on restaurant operations, or implement pricing, billing, or expiration.
Those rules will be added with subscriptions. Development Hibernate schema update creates the
`plan` table; no migration tool is introduced.

## Current business access

`CurrentBusinessService.requireActiveBusiness()` requires the `owner` or `waiter` role,
looks up membership using the authenticated JWT subject, and returns the active business
summary. Missing membership returns `403` with code `Membership.Required`; an inactive
business returns `403` with code `Business.Inactive`. The caller supplies no business or user ID.

`GET /api/me/owner-check` additionally requires the `owner` role and invokes this check.
`GET /api/me` remains a profile endpoint: users without membership, including platform
administrators, can still retrieve their profile. Administrative endpoints remain independent
of operational business membership.

Future operational services must invoke this check and scope resource queries to the returned
business ID. A caller-supplied product, table, or order ID is not proof of business ownership.
For writes, perform the check and the persistence operation within the same service transaction.
The current access check does not grant permissions for specific actions beyond its role check;
each endpoint must also enforce the roles required for its operation.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Bean validation using Hibernate Validator and Jakarta Validation annotations
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Generate OpenAPI schemas and serve Swagger UI for REST API documentation
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- OpenID Connect ([guide](https://quarkus.io/guides/security-openid-connect)): Secure applications with OpenID Connect and OAuth 2.0 using bearer tokens and authorization code flow
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplified JPA/Hibernate data access layer with active record and repository patterns
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
