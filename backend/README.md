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

Plans can be made inactive through editing; there is no delete route. The catalog does not
enforce resource usage limits or implement pricing or billing. Development Hibernate schema
update creates the `plan` table; no migration tool is introduced.

## Subscriptions

Admin-only routes under `/api/admin/businesses/{businessId}/subscription`:

- `GET`: current subscription, or `204` if none is assigned; a missing business returns `404`.
- `POST`: assign once with `planId`, `startsAt`, and `expiresAt`; returns `201`.
- `POST /renewals`: renew with `planId`, `expiresAt`, and optional `startsAt`.
- `PUT /plan`: change `planId` while preserving the dates. Selecting the same plan is a no-op.
- `GET /history?page=0&size=10`: paginated assignment, renewal, and plan change history.

Dates are UTC instants. Status is calculated on the server: `NOT_STARTED` before the start,
`ACTIVE` when `startsAt <= now < expiresAt`, otherwise `EXPIRED`. No scheduled job or stored
expiration flag is needed. Assignment and renewal require an end later than both start and now.
Renewing before expiration preserves the start and must extend the end. After expiration,
the start defaults to server time; an explicit start cannot precede the previous expiration.
A future start keeps operational access blocked until that instant.

New selections require an active plan. Its name and limits are copied into subscription terms;
catalog edits or deactivation do not change existing terms. Renewal copies the selected plan's
latest terms. Changing plans alone does not renew an expired subscription.
Each change and its history snapshot, actor, and timestamp are saved in one transaction.
Business row locking serializes subscription changes; runtime exceptions roll back automatically.
Development schema update creates `subscription` and `subscription_history`.

Business suspension remains independent: renewing does not reactivate a suspended business.
Existing businesses require an initial assignment to regain operational access.
Table, product, and team membership creation enforce the subscribed limits.
Existing data is preserved after a downgrade. Billing, trials, and general administrative
auditing remain pending. No tests were added for this feature; existing operational fixtures will
need a valid subscription when their tests are updated.

## Current business access

`CurrentBusinessService.requireActiveBusiness()` requires the `owner` or `waiter` role,
looks up membership using the authenticated JWT subject, and returns the active business
summary. Missing membership returns `403` with code `Membership.Required`; an inactive
business returns `403` with code `Business.Inactive`. It also requires a current subscription:
missing, future, and expired subscriptions return `403` with `Subscription.Required`,
`Subscription.NotStarted`, and `Subscription.Expired`, respectively.
The caller supplies no business or user ID.

`GET /api/me/owner-check` additionally requires the `owner` role and invokes this check.
`GET /api/me` remains a profile endpoint: users without membership, including platform
administrators, can still retrieve their profile, including a nullable `subscription` summary.
Expired subscriptions do not block this endpoint. Administrative endpoints remain independent
of operational business membership.

Future operational services must invoke this check and scope resource queries to the returned
business ID. A caller-supplied product, table, or order ID is not proof of business ownership.
For writes, perform the check and the persistence operation within the same service transaction.
The current access check does not grant permissions for specific actions beyond its role check;
each endpoint must also enforce the roles required for its operation.

## Restaurant table configuration

The `restaurant.tables` feature provides owner-only routes scoped to the authenticated user's
membership. No request accepts a business ID:

- `GET /api/tables?page=0&size=10`: paginated tables ordered by name and ID.
- `POST /api/tables`: create a table (`201`).
- `PUT /api/tables/{id}`: edit its name and capacity (`200`).
- `DELETE /api/tables/{id}`: remove a table (`204`).

Create and edit accept `{ "name": "Mesa 1", "capacity": 4 }`. Names are trimmed, required,
up to 100 characters, and unique within the business without case distinction. Capacity must be
a positive integer. Responses contain `id`, `name`, and `capacity`. Foreign-business IDs return
the same `404` (`Table.NotFound`) as missing IDs. Duplicate names return `409`
(`Table.DuplicateName`); a full plan returns `422` (`Table.LimitReached`).

Every operation requires an active business and current subscription. Writes lock the business
row before rechecking access, changing tables, or counting usage, so concurrent creates cannot
exceed `maxTables`. Subscription changes use the same business lock. A downgrade preserves
tables and permits edits/deletions; new tables are blocked while usage meets or exceeds the limit.
Hibernate development schema update creates `dining_table` with a business/name unique constraint.
Development CORS now permits `DELETE`.

Operational occupancy is exposed through `/api/orders/tables`. Tables referenced by any order
cannot be deleted (`409`, `Table.HasOrders`); the order foreign key also preserves this relation.
No tests were added or run for this feature.

## Product catalog

The `restaurant.catalog` feature scopes all queries and writes to the authenticated membership:

- `GET /api/products?page=0&size=10`: owner/waiter listing, ordered by name and ID.
  Optional `search` (up to 100 characters) filters name and description, ignoring case;
  optional `available=true|false` filters availability. Filters apply before counting and
  pagination and always remain scoped to the authenticated user's business. For example,
  `/api/products?search=cafe&available=true&page=0&size=12`.
- `POST /api/products`: owner-only creation (`201`).
- `PUT /api/products/{id}`: owner-only editing (`200`).

Example request:

```json
{"name":"Cappuccino","description":"Café con leche y espuma","price":12.50,"available":true}
```

Names are trimmed, required, up to 100 characters, and unique within the business ignoring case.
Description is optional (up to 1000 characters); omitted or null descriptions become empty strings.
Price is a positive `BigDecimal` from `0.01` to `9999999999.99`, with at most two decimal places,
stored as `NUMERIC(12,2)` without rounding. `available` is required. Responses contain `id` and
these four fields. Currency selection and conversion are not implemented.

All operations require an active business and current subscription. Foreign-business and missing
product IDs return `404` (`Product.NotFound`); duplicate names return `409`
(`Product.DuplicateName`); reaching `maxProducts` returns `422` (`Product.LimitReached`).
Creation counts all products, including unavailable ones. Business row locking serializes writes
with subscription changes and concurrent creates. A downgrade preserves existing products and
allows editing them; new products are blocked while usage meets or exceeds the limit.

No product deletion route is provided; owners change availability through editing. Waiters can
read both available and unavailable entries but cannot write. Ordering validates availability
and snapshots the authoritative price on the server. Categories, images, and promotions
remain pending. Hibernate development schema update creates `product`.
No tests were added or run for this feature.

## Open orders by table

The `restaurant.orders` feature allows both `owner` and `waiter` to operate on their own business:

- `GET /api/orders/tables?page=0&size=12`: paginated tables with nullable `orderId`, total,
  and unit count. A non-null `orderId` means occupied; no separate occupancy flag is stored.
- `POST /api/orders` with `{ "tableId": "UUID" }`: open an empty order (`201`).
- `GET /api/orders/{id}`: retrieve the order, table, revision, opener, opening time, items, and total.
- `PUT /api/orders/{id}/items/{productId}` with `{ "quantity": 2, "revision": 0 }`:
  set the absolute quantity and return the updated order. Zero removes the line.
- `PUT /api/orders/{id}/close` with `{ "revision": 1 }`: close the order and release its table.
- `GET /api/orders/history?page=0&size=10`: closed orders, newest closing time first.

Quantities must be integers from 0 to 999; revisions are nonnegative integers. Clients never
submit names, prices, totals, or business IDs. New lines snapshot the current product name and
`BigDecimal` price. Existing lines retain those terms when quantities change. Removing a line
and adding it again uses the latest catalog terms. Products must be available when adding or
increasing quantities; reducing or removing an existing unavailable product is allowed.
Line totals and the order total are computed from persisted prices on the server.

Every read/write requires membership, an active business, and a current subscription. Missing
and foreign-business orders return the same `404` (`Order.NotFound`). Writes serialize through
the shared business lock, including table/catalog/subscription changes. A unique nullable
`open_table_id` slot also prevents multiple open orders per table at the database level.
Opening an occupied table returns `409` (`Order.TableOccupied`); refresh tables to find its order.

Each item change checks the supplied revision and increments it atomically. A stale revision
returns `409` (`Order.Changed`); refresh and review the order before submitting again. Repeating
an unchanged quantity is a no-op. All changes use standard transactional rollback and automatic
flush at commit. Hibernate development schema update creates `restaurant_order` and `order_item`.

Orders transition from `OPEN` to `CLOSED`. Closing checks the revision, records server UTC time
and the authenticated closer's name/subject, clears `open_table_id`, and increments the revision
in the same transaction. Empty orders may also close. Closing an already closed order returns
its original result without changing metadata or releasing any newer order's table slot.
Closed orders reject item changes (`409`, `Order.NotOpen`) and remain available by ID and in
history. No reopening or order deletion is provided. Kitchen, payment, and cancellation remain
pending; closing does not register payment. No tests were added or run for this feature.

Existing development databases may still have the original check that allows only `OPEN`.
Apply the checked-in SQL once from the repository root before using closing:

```sh
docker compose exec -T postgres psql -U goshop -d goshop -v ON_ERROR_STOP=1 < backend/database/allow-closed-orders.sql
```

This widens the check to `OPEN`/`CLOSED` without deleting data or introducing Flyway. Fresh
databases receive both values from the current enum mapping. Restart/reload Quarkus in development
so Hibernate schema update also adds nullable `closed_at`, `closed_by`, and `closed_by_subject`.

## Team management

`platform.membership` exposes owner-only routes for the current business:

- `GET /api/team?page=0&size=10`: paginated memberships, including the owner.
- `GET /api/team/available-waiters?search=juan&page=0&size=10`: available accounts with
  `id`, `username`, and `displayName`. Search is optional; pagination is over eligible accounts.
- `POST /api/team` with `{ "keycloakUserId": "KEYCLOAK_USER_ID" }`: link the selected account (`201`).
- `DELETE /api/team/{membershipId}`: unlink a waiter (`204`).

Both the JWT `owner` role and an `OWNER` membership are required, along with an active business
and current subscription. Clients cannot choose the business or assigned role. The owner cannot
be removed (`422`, `Membership.OwnerProtected`); missing and foreign-business memberships return
the same `404` (`Membership.NotFound`).

Onboarding looks up the submitted ID through the existing Keycloak technical client outside
the database transaction. The account must exist, be enabled, have the effective realm role
`waiter`, and have neither `owner` nor `admin`. The API does not create accounts, change roles,
or send invitations. The technical account needs permission to search users and read their
effective realm roles. Provider failures retain the existing `503` handling.

The picker excludes accounts with membership in any business and applies the same enabled/role
rules. It searches enabled Keycloak accounts in batches of 100, skips assigned IDs before reading
effective roles, sorts eligible accounts by username/ID, and paginates after filtering, so pages
and totals do not include ineligible users. Display names use first/last name with username as
fallback; email and other account details are not returned. The query scans matching accounts
on every request to keep eligibility and totals current. For large realms, replace that scan
with an indexed directory rather than caching assignment authority in the browser.

Picker results are advisory: assignment revalidates the ID, enabled state, roles, uniqueness,
and quota. If another business links the selected user meanwhile, the write returns `409`;
refresh the picker and select another account. The old username-based POST body is no longer accepted.

Persistence rechecks owner access, membership uniqueness, and the subscribed `maxUsers` under
the business write lock. The owner and all waiters count toward that limit. A downgrade preserves
memberships and permits unlinking, but blocks additions while usage meets or exceeds the limit.
A transaction-scoped PostgreSQL advisory lock per Keycloak user also serializes assignments
across businesses, including owner onboarding; the unique `keycloak_user_id` constraint remains
the final guarantee. Conflicts return `409` (`Membership.UserAlreadyAssigned`).

Unlinking removes only the local membership, retains the Keycloak account and order history,
and frees a user slot. Operational writes recheck membership after acquiring the business lock,
so a waiter removed while waiting cannot proceed with the old business assignment. Existing
tokens retain their realm roles but do not grant business access without membership.

Responses include membership ID, Keycloak user ID, nullable username, role, and joining time.
The username is a display snapshot captured at linking; old memberships can have no snapshot.
Development Hibernate schema update adds the nullable `membership.username` column.
No tests were added or run for this feature.

## Business overview

`GET /api/overview` provides the Resumen page for `owner` and `waiter`. It uses the current
membership and the existing active-business/current-subscription checks; no business ID is
accepted from the client. The read-only `restaurant.overview` service coordinates the order and
catalog application services without accessing their repositories directly.

The response includes:

- `generatedAt`: server UTC time when the response was assembled.
- `metrics`: global `totalTables`, `occupiedTables`, `openOrders`, and `availableProducts` counts.
- `tables`: the first 12 tables with their open order, unit count, and total.
- `recentOrders`: the five most recently opened orders that are still open.
- `activity`: the ten most recently updated orders, including open and closed orders, with
  table name, product/quantity summary, state, timestamps, and server-calculated total.

Counters cover the whole business, independently of the bounded lists. Occupied tables equal
open orders under the one-open-order-per-table rule. Available products count only enabled
catalog entries. The response is not restricted to today's orders: open orders remain visible
until closed. Activity summarizes each order's current state; it is not an immutable event log.
The frontend refreshes manually; there is no polling, kitchen status, payment, or revenue metric.
No schema changes, dependencies, or tests were added for this feature; tests were not run.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Bean validation using Hibernate Validator and Jakarta Validation annotations
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Generate OpenAPI schemas and serve Swagger UI for REST API documentation
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- OpenID Connect ([guide](https://quarkus.io/guides/security-openid-connect)): Secure applications with OpenID Connect and OAuth 2.0 using bearer tokens and authorization code flow
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplified JPA/Hibernate data access layer with active record and repository patterns
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
