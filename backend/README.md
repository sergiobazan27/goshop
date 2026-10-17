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
