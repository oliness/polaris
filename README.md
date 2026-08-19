# Police K9 Dogs API

A RESTful API over the register of dogs serving with a police force, built with Micronaut,
Micronaut Data JDBC, Flyway and MapStruct. It implements the task in
[`interview_coding_task.md`](interview_coding_task.md).

Records are never physically removed. A `DELETE` stamps `deletedAt`, which keeps the audit
history intact while hiding the record from the list endpoints by default.

## Prerequisites

A **JDK 25** toolchain - Micronaut 5's Gradle plugin requires it, and `build.gradle.kts` pins it:

```bash
java -version   # should report 25
```

Nothing else. The Gradle wrapper fetches Gradle itself, and the database is in-memory H2 created by
the Flyway migrations on start-up, so there is nothing to install or configure.

## Running it

```bash
./gradlew run
```

The application starts on <http://localhost:8080> with a small sample register already loaded, so
there is something to look at without creating it first.

| What | Where |
| --- | --- |
| Swagger UI | <http://localhost:8080/swagger-ui> |
| Redoc | <http://localhost:8080/redoc> |
| RapiDoc | <http://localhost:8080/rapidoc> |
| OpenAPI Explorer | <http://localhost:8080/openapi-explorer> |
| OpenAPI document | <http://localhost:8080/swagger/police-k9-dogs-api-1.0.0.yml> |

```bash
curl 'http://localhost:8080/api/dogs/dogs?filter={"breed":"malinois"}'
```

To build and run a standalone jar instead:

```bash
./gradlew build
MICRONAUT_ENVIRONMENTS=dev java -jar build/libs/dogs-1.0.0-all.jar
```

`./gradlew run` sets that environment for you; without it the register starts empty, which is how
the tests and a real deployment run.

## Running the tests

```bash
./gradlew test
```

The suite covers the service rules with mocks, the constraints against the validator, and every
endpoint over HTTP against a real database built by the same migrations as production.

## The API

Every endpoint sits under `/api/dogs`, consumes and produces `application/json`, and needs no
authorisation.

| Resource | Path | Methods |
| --- | --- | --- |
| Dogs | `/api/dogs/dogs` | `GET` `POST` `GET /{id}` `PUT /{id}` `DELETE /{id}` |
| Suppliers | `/api/dogs/suppliers` | `GET` `POST` `GET /{id}` `PUT /{id}` `DELETE /{id}` |
| Dog statuses | `/api/dogs/statuses` | `GET` `POST` `GET /{id}` `PUT /{id}` `DELETE /{id}` |
| Leaving reasons | `/api/dogs/leaving-reasons` | `GET` `POST` `GET /{id}` `PUT /{id}` `DELETE /{id}` |
| Genders | `/api/dogs/genders` | `GET` |

`POST` answers `201` with a `Location` header pointing at the new record. `DELETE` answers `204`
and changes nothing the second time, so repeating it is safe.

The server root is the one address outside `/api/dogs`: `GET /` answers `303 See Other` pointing
at the dogs list, so opening the address the server logs on start-up shows the register rather
than a `404`. It is a signpost for a human, not part of the published contract, and is left out of
the OpenAPI document.

### Registering a dog

Relationships are supplied as identifiers, discoverable from `/api/dogs/suppliers`,
`/api/dogs/statuses` and `/api/dogs/leaving-reasons`:

```bash
curl -X POST http://localhost:8080/api/dogs/dogs \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Baxter",
        "breed": "German Shepherd",
        "supplierId": 1,
        "badgeId": "K9-1041",
        "gender": "MALE",
        "birthDate": "2020-03-14",
        "dateAcquired": "2021-01-06",
        "statusId": 2,
        "kennellingCharacteristic": "Settles quickly. Must not be kennelled next to entire males."
      }'
```

`badgeId`, `kennellingCharacteristic`, `leavingDate` and `leavingReasonId` are optional and the
rest are required. A dog that has left needs `leavingDate` and `leavingReasonId` together; one that
is still serving needs neither. `gender` takes a code from `GET /api/dogs/genders`.

### Editing without overwriting someone else

Fetching a single record returns an `ETag` - the record's version as an entity tag. Send it back as
`If-Match` on the `PUT` and the update is refused with `412` if anyone changed the record in
between, rather than silently replacing their work:

```bash
curl -i http://localhost:8080/api/dogs/dogs/7
# ETag: "3"

curl -X PUT http://localhost:8080/api/dogs/dogs/7 \
  -H 'Content-Type: application/json' \
  -H 'If-Match: "3"' \
  -d '{ "name": "Baxter", ... }'
```

```json
{ "status": 412,
  "message": "Dog 7 has changed since you read it. Fetch it again and re-apply the change." }
```

A successful `PUT` answers with the new `ETag`, so a client can keep editing without re-fetching.
`POST` returns one too, alongside `Location`.

`If-Match` is optional: omit it, or send `If-Match: *`, and the update applies unconditionally. A
tag this API could not have issued simply fails to match, so a malformed one is refused rather than
quietly ignored. `409` remains what a *conflict* means here - a name or badge already taken, or a
record kept only for audit - while `412` means only that the precondition did not hold.

### Searching

The dogs list takes the `filter` query parameter as JSON, as the task specifies:

```
GET /api/dogs/dogs?filter={"name":"bax","breed":"shepherd","supplier":"ravenscroft"}
```

Each term is an optional, case-insensitive "contains" match, and the terms are combined with AND.
A wildcard the caller happens to type is escaped rather than honoured, so searching for `%` finds
dogs called `%`. A key that is not one of the three is rejected with `400` rather than ignored, so
a mistyped term fails loudly instead of silently widening the search.

### Paging and sorting

Every list endpoint is paged, and answers with the page plus the totals a client needs to ask for
the next one:

```
GET /api/dogs/dogs?page=1&size=20&sort=name,desc
```

```json
{ "content": [], "page": 1, "size": 20, "totalElements": 137, "totalPages": 7 }
```

The default page size is 20 and the maximum is 100; a caller who asks for more is given 100 rather
than an error, so one client cannot ask for the whole register at once. When no `sort` is given a
stable default is applied, because without one the database is free to reorder rows between
requests and page 2 can repeat or skip what was on page 1.

### Deleted records

`DELETE` marks a record and nothing more. The list endpoints leave marked records out unless
`?includeDeleted=true` is passed, while `GET /{id}` still returns one - an auditor holding an
identifier has to be able to read it - flagged with `"deleted": true`.

A deleted record can no longer be changed or assigned: a dog cannot be sourced from a deleted
supplier or given a retired status, and a deleted dog cannot be updated. Dogs already pointing at a
retired supplier or status keep pointing at it, which is what makes the history readable later.

### Errors

Every failure - including the framework's own - comes back in one shape, so a client only has to
parse one thing:

```json
{
  "timestamp": "2026-08-18T09:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "The request failed validation",
  "path": "/api/dogs/dogs",
  "details": [{ "field": "birthDate", "message": "must be a past date" }]
}
```

`details` names each rejected field separately so a client can attach a message to the input that
caused it rather than parsing prose. A rule spanning several fields has no single field to blame,
so it is reported as the headline `message` instead.

| Status | When |
| --- | --- |
| `400` | The request failed validation, or a parameter would not convert |
| `404` | Nothing has ever had that identifier |
| `409` | The badge, name or code is taken, the record is deleted, or two writes collided |
| `412` | `If-Match` no longer matches - someone changed the record since you read it |

## Project layout

```
uk.police.k9.dogs
├── config       the clock, and the converter that binds the filter query parameter
├── controller   the HTTP endpoints: bind, validate, delegate, respond
├── dto          request and response records, and the paged wrapper
├── entity       the persisted types, on a shared AuditedEntity base
├── exception    domain exceptions, their HTTP handlers, and the shared ApiError shape
├── mapper       MapStruct entity <-> DTO mapping
├── repository   Micronaut Data repositories, with spec/ for the query criteria
├── service      the rules the register enforces
└── validation   the cross-field constraint on a dog's dates
```

The tests mirror this, with `support/` holding the request builder they share.

## Design decisions

**Statuses and leaving reasons are tables, not enums.** The task lists the values that are
*currently* possible, which reads as an expectation that the force will add more. They are
therefore rows maintained through `/api/dogs/statuses` and `/api/dogs/leaving-reasons`, so the next
value needs neither a code change nor a release.

**Gender stays a Java enum.** It is the one enumerated field on a dog that is a genuinely closed
set. It is still published over HTTP at `/api/dogs/genders`, in the same `code`/`label` shape as
the maintainable lookups, so a client can build every drop-down the same way instead of hard-coding
values and drifting.

**Suppliers are their own resource.** The task says more than one dog may come from the same
supplier, so a supplier is a record with its own endpoints - its contact details are corrected in
one place rather than on every dog.

**Soft delete everywhere.** Every table carries `version`, `createdAt`, `updatedAt` and
`deletedAt`. Nothing issues a SQL `DELETE`.

**Optimistic locking over `ETag`/`If-Match`, checked in two places.** The version travels as an
entity tag rather than a field in the body, because that is what HTTP already has for this and it
keeps the concern out of the resource representation - a `PUT` body stays the dog, not the dog plus
bookkeeping. A failed precondition is `412`, per RFC 9110, which distinguishes "you are working
from a stale copy" from the `409` conflicts the register itself raises. Independently, the
`version` column guards the database, so even two transactions that interleave cannot silently
overwrite one another - that one surfaces as `409`, being a genuine collision rather than a
precondition the caller could have checked.

**MapStruct with `unmappedTargetPolicy=ERROR`.** Mapping is generated rather than hand-written, and
leaving a target field unmapped fails the build - so adding a column can no longer silently fail to
reach the API, which is the reason to use MapStruct at all rather than a nicety.

**A project-owned `PagedResponse`.** The JSON shape of a page is part of this application's
published contract, so it does not change when the framework's own `Page` does.

**The `filter` parameter is bound by a converter**, not parsed inside the controller. The
controller signature declares that it takes a `DogFilter`, and malformed JSON is rejected during
binding, before any controller code runs.

## Assumptions

* A badge identifier is held by at most one serving dog at a time; deleted dogs keep theirs, so the
  audit trail still shows who carried it. The same applies to supplier names and lookup codes.
* `badgeId` is optional - a dog in training has not been badged yet - as are the kennelling
  characteristic and the supplier's contact details.
* A leaving date and a leaving reason travel together: a dog that has left needs both, one that is
  still serving needs neither. A dog cannot be acquired before it was born, or leave before it was
  acquired.
* `PUT` replaces a record rather than patching it, which is what the method means.

## If this were going further

* Postgres in place of in-memory H2, which is the only reason the dialect is pinned to H2.
* Who changed a record, not just when - the audit columns answer "when" but not "by whom", which
  needs authentication the task rules out.
* A dog's status history, so the register could answer when a dog went into service rather than
  only what it is now.
