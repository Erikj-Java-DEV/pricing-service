# Pricing Service

REST API developed with Java and Spring Boot to resolve the applicable price for a product and brand at a specific date.

The project uses **Hexagonal Architecture (Ports and Adapters)** to keep business and application logic independent from HTTP, Spring Data JPA and database-specific concerns.

---

## Functional Requirements

The service receives:

- Application date and time.
- Product identifier.
- Brand identifier.

A price is applicable when:

```text
brandId = requested brand
productId = requested product
startDate <= applicationDate
endDate >= applicationDate
```

If several prices are applicable simultaneously, the price with the **highest priority** must be returned.

Only one result is returned.

The filtering, priority ordering and result limitation are executed at database level rather than loading multiple records and resolving them in application memory.

---

## Technologies

| Technology | Purpose |
|---|---|
| Java 21 | Main programming language |
| Spring Boot 4.1.1 | Application bootstrap, dependency management and auto-configuration |
| Spring Web MVC | REST API implementation |
| Spring Data JPA | Persistence abstraction |
| Hibernate | JPA implementation |
| H2 | In-memory relational database required by the exercise |
| Bean Validation | Validation of incoming HTTP parameters |
| Maven | Build and dependency management |
| JUnit / JUnit Jupiter | Unit and integration testing |
| Mockito | Isolation of application dependencies in unit tests |
| AssertJ | Fluent assertions |
| MockMvc | HTTP integration testing without an external web server |
| OpenAPI 3 | REST contract documentation |
| JaCoCo | Test coverage measurement and reporting |

### Why H2?

The exercise requires an in-memory database.

H2 allows the complete application and test suite to execute without external infrastructure while still exercising a real relational persistence layer.

### Why Spring Data JPA?

Persistence remains behind an output port.

The application layer does not know that Spring Data JPA or H2 are being used. The persistence implementation can therefore be replaced without modifying the application use case.

---

# Architecture

The application follows **Hexagonal Architecture**, also known as **Ports and Adapters**.

The main objective is to isolate the application core from external technologies.

```text
                         Infrastructure

     HTTP Request
          |
          v
   PriceController
    REST Adapter
          |
          v

+-----------------------------------------------+
|                Application                    |
|                                               |
|   GetApplicablePriceUseCase   <- Input Port   |
|              |                                |
|              v                                |
|   GetApplicablePriceService                   |
|              |                                |
|              v                                |
|   LoadApplicablePricePort     <- Output Port  |
|                                               |
+-----------------------------------------------+
          |
          v
 PricePersistenceAdapter
    Persistence Adapter
          |
          v
 SpringDataPriceRepository
          |
          v
       H2 / JPA
```

---

## Domain Layer

```text
domain
└── model
    └── Price
```

The domain contains the business model.

`Price` represents a price independently from:

- HTTP.
- Spring.
- Spring Data.
- JPA.
- Hibernate.
- H2.

The domain also protects basic invariants:

- Mandatory fields cannot be `null`.
- `startDate` cannot be after `endDate`.

No framework annotations exist in the domain model.

---

## Application Layer

```text
application
├── exception
├── port
│   ├── in
│   └── out
└── service
```

The application layer contains the business use case and defines the boundaries through which infrastructure interacts with it.

### Input Port

```text
GetApplicablePriceUseCase
```

Defines the operation exposed by the application.

The REST controller depends on this interface instead of depending directly on the implementation.

### Application Service

```text
GetApplicablePriceService
```

Implements the use case.

Its responsibility is to request the applicable price through the output port and raise an application exception when no result exists.

It does not know how or where prices are stored.

### Output Port

```text
LoadApplicablePricePort
```

Defines what the application requires from persistence.

The application service depends on this interface instead of Spring Data JPA.

---

## Infrastructure Layer

```text
infrastructure
├── adapter
│   ├── in
│   │   └── rest
│   └── out
│       └── persistence
└── config
```

Infrastructure contains all framework and technology-specific implementations.

### REST Adapter

```text
PriceController
PriceResponse
GlobalExceptionHandler
ApiErrorResponse
```

Responsibilities:

- Receive HTTP requests.
- Parse the application date.
- Validate identifiers.
- Execute the application use case.
- Map the domain response to the API representation.
- Translate application and validation errors into HTTP responses.

### Persistence Adapter

```text
PricePersistenceAdapter
SpringDataPriceRepository
PriceJpaEntity
PricePersistenceMapper
```

Responsibilities:

- Implement the application output port.
- Execute the database query.
- Apply filtering and priority ordering.
- Convert the JPA representation into the domain model.

JPA does not leak into the domain or application layers.

---

# Dependency Direction

Dependencies always point toward the application core.

```text
PriceController
      |
      v
GetApplicablePriceUseCase
      |
      v
GetApplicablePriceService
      |
      v
LoadApplicablePricePort
      ^
      |
PricePersistenceAdapter
      |
      v
SpringDataPriceRepository
```

The application layer never imports:

```text
Spring MVC
Spring Data JPA
Hibernate
H2
HTTP-specific types
```

For example, H2 could be replaced by PostgreSQL without changing:

```text
Price
GetApplicablePriceUseCase
GetApplicablePriceService
LoadApplicablePricePort
```

Only the persistence adapter and infrastructure configuration would need to change.

---

# Model Separation

The project deliberately separates three representations.

## Domain

```text
Price
```

Represents the business concept.

## REST

```text
PriceResponse
```

Represents the public HTTP response.

## Persistence

```text
PriceJpaEntity
```

Represents the relational persistence model and contains JPA annotations.

Persistence mapping:

```text
PriceJpaEntity
       |
       v
PricePersistenceMapper
       |
       v
Price
```

HTTP mapping:

```text
Price
  |
  v
PriceResponse
```

This prevents persistence or API concerns from dictating the structure of the domain model.

---

# Complete Request Flow

A successful request follows this path:

```text
GET /api/v1/prices
        |
        v
PriceController
        |
        v
GetApplicablePriceUseCase
        |
        v
GetApplicablePriceService
        |
        v
LoadApplicablePricePort
        |
        v
PricePersistenceAdapter
        |
        v
SpringDataPriceRepository
        |
        v
H2
```

The database result is mapped to `Price`, returned through the application layer and finally converted to `PriceResponse`.

---

# Price Resolution Strategy

The persistence query performs the business lookup using:

```text
brandId = requested brand
productId = requested product
startDate <= applicationDate
endDate >= applicationDate
```

When multiple records match:

```text
ORDER BY priority DESC
```

is applied.

Only the first result is selected.

Conceptually:

```sql
SELECT *
FROM prices
WHERE brand_id = ?
  AND product_id = ?
  AND start_date <= ?
  AND end_date >= ?
ORDER BY priority DESC
FETCH FIRST 1 ROW ONLY;
```

This avoids an implementation such as:

```text
load all applicable rows
→ filter in Java
→ sort in Java
→ select first
```

and keeps extraction efficient by delegating filtering, ordering and limitation to the database.

---

# API

## Get Applicable Price

```http
GET /api/v1/prices
```

### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `applicationDate` | ISO-8601 local date-time | Yes | Date and time used to resolve the applicable price |
| `productId` | Long | Yes | Product identifier |
| `brandId` | Long | Yes | Brand identifier |

Example:

```http
GET /api/v1/prices?applicationDate=2020-06-14T16:00:00&productId=35455&brandId=1
```

Response:

```json
{
  "productId": 35455,
  "brandId": 1,
  "priceList": 2,
  "startDate": "2020-06-14T15:00:00",
  "endDate": "2020-06-14T18:30:00",
  "price": 25.45
}
```

---

# Error Handling

The API uses a consistent JSON error response.

Possible statuses:

```text
200 OK
```

Applicable price found.

```text
400 Bad Request
```

Invalid identifier, invalid date format or missing required parameter.

```text
404 Not Found
```

No applicable price exists for the requested product, brand and date.

Example:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "No applicable price found...",
  "timestamp": "2020-06-14T16:00:00"
}
```

Integration tests verify not only the HTTP status but also the JSON error contract.

---

# Database

The application uses an embedded H2 database.

The schema is defined explicitly in:

```text
src/main/resources/schema.sql
```

Initial exercise data is loaded from:

```text
src/main/resources/data.sql
```

The dataset contains the four price records defined by the technical exercise.

An index is also created for the columns involved in price lookup.

---

# Application Configuration

The configuration is intentionally minimal:

```yaml
spring:
  application:
    name: pricing-service

  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
```

This is deliberate rather than missing configuration.

## H2 auto-configuration

No explicit datasource URL, driver, username or password is configured.

Because H2 is available as a runtime dependency and JDBC/JPA support is present, Spring Boot automatically configures an embedded datasource.

Therefore these properties are intentionally omitted:

```text
spring.datasource.url
spring.datasource.driver-class-name
spring.datasource.username
spring.datasource.password
```

This avoids duplicating values that Spring Boot can derive automatically.

It also lets Spring Boot generate a unique embedded database name rather than forcing all application contexts to reuse a manually named in-memory database.

## SQL initialization

No explicit:

```text
spring.sql.init.mode
```

is required.

For an embedded database, Spring Boot's default initialization mode is `embedded`.

Therefore the conventional files:

```text
schema.sql
data.sql
```

are automatically detected and executed.

## `ddl-auto: none`

This property is explicitly configured:

```yaml
hibernate:
  ddl-auto: none
```

because the database schema is owned by:

```text
schema.sql
```

Hibernate must therefore not create, modify or drop the schema.

This avoids having two competing schema-generation mechanisms.

## `open-in-view: false`

Spring Boot enables Open EntityManager in View by default for web applications.

This project disables it explicitly:

```yaml
open-in-view: false
```

because persistence access is completed inside the persistence/application flow and the REST layer does not require lazy loading.

It also prevents persistence context lifetime from being unnecessarily extended through the HTTP request.

## Other omitted defaults

Properties such as:

```text
spring.jpa.show-sql=false
spring.sql.init.mode=embedded
spring.datasource.generate-unique-name=true
```

are not repeated because they already represent the desired default behavior.

The configuration therefore contains only settings where this application deliberately changes or fixes behavior.

---

# Testing Strategy

The project uses several testing levels.

The intention is not simply to increase a coverage percentage but to verify behavior at the appropriate architectural boundary.

---

## Domain Tests

### `PriceTest`

Pure Java tests. No Spring context is loaded.

| Test | Purpose |
|---|---|
| `shouldRejectPriceWhenStartDateIsAfterEndDate` | Verifies that the domain rejects an invalid validity period |
| `shouldRejectPriceWhenRequiredFieldIsNull` | Verifies the mandatory-field domain invariant |

These tests ensure domain rules work independently from Spring or persistence.

---

## Application Unit Tests

### `GetApplicablePriceServiceTest`

Uses Mockito and does not load Spring.

| Test | Purpose |
|---|---|
| `shouldReturnApplicablePriceWhenPriceExists` | Verifies that the use case returns the price supplied by the output port |
| `shouldThrowPriceNotFoundExceptionWhenPriceDoesNotExist` | Verifies application behavior when persistence returns no applicable price |

`LoadApplicablePricePort` is mocked so these tests validate only application behavior.

---

## Persistence Tests

### `SpringDataPriceRepositoryTest`

Uses `@DataJpaTest` with an embedded database.

These tests verify the actual repository query rather than mocking persistence.

| Test | Purpose |
|---|---|
| `shouldReturnHighestPriorityPriceWhenSeveralPricesAreApplicable` | Verifies that overlapping prices are resolved using the highest priority |
| `shouldReturnEmptyWhenNoPriceIsApplicable` | Verifies that no result is returned outside all validity periods |
| `shouldReturnPriceWhenApplicationDateMatchesStartDate` | Verifies that `startDate` is inclusive (`<=`) |
| `shouldReturnPriceWhenApplicationDateMatchesEndDate` | Verifies that `endDate` is inclusive (`>=`) |
| `shouldNotReturnPriceFromAnotherBrand` | Verifies isolation by `brandId` |
| `shouldNotReturnPriceFromAnotherProduct` | Verifies isolation by `productId` |

These tests specifically validate the semantics on which the database query depends.

---

## Integration Tests

### `PriceControllerIntegrationTest`

Uses:

```java
@SpringBootTest
@AutoConfigureMockMvc
```

The persistence repository is **not mocked**.

Therefore these tests exercise:

```text
HTTP
→ Controller
→ Input Port
→ Application Service
→ Output Port
→ Persistence Adapter
→ Spring Data JPA
→ H2
```

### Mandatory Exercise Scenarios

| Test | Request | Expected tariff |
|---|---|---|
| `test1_shouldReturnPriceList1At10OnJune14` | 2020-06-14 10:00 | Price list 1 / 35.50 EUR |
| `test2_shouldReturnPriceList2At16OnJune14` | 2020-06-14 16:00 | Price list 2 / 25.45 EUR |
| `test3_shouldReturnPriceList1At21OnJune14` | 2020-06-14 21:00 | Price list 1 / 35.50 EUR |
| `test4_shouldReturnPriceList3At10OnJune15` | 2020-06-15 10:00 | Price list 3 / 30.50 EUR |
| `test5_shouldReturnPriceList4At21OnJune16` | 2020-06-16 21:00 | Price list 4 / 38.95 EUR |

Each scenario verifies:

```text
productId
brandId
priceList
startDate
endDate
price
```

and not merely the HTTP status.

### Error Scenarios

| Test | Purpose |
|---|---|
| `shouldReturnNotFoundWhenNoApplicablePriceExists` | Verifies the `404` response when no price is applicable |
| `shouldReturnBadRequestWhenProductIdIsInvalid` | Verifies validation of a non-positive product identifier |
| `shouldReturnBadRequestWhenBrandIdIsInvalid` | Verifies validation of a non-positive brand identifier |
| `shouldReturnBadRequestWhenApplicationDateIsInvalid` | Verifies handling of an invalid date format |
| `shouldReturnBadRequestWhenRequiredParameterIsMissing` | Verifies handling of a missing required parameter |

The error tests also verify:

```text
Content-Type
status
error
non-empty message
timestamp
```

Exact internal Spring error messages are intentionally not asserted because that would couple tests to framework implementation details.

---

# Test Coverage

JaCoCo is integrated with the Maven build.

Run:

```bash
./mvnw clean verify
```

or on Windows:

```powershell
.\mvnw.cmd clean verify
```

The HTML report is generated at:

```text
target/site/jacoco/index.html
```

JaCoCo is used as a diagnostic tool to detect meaningful untested areas.

The project deliberately does not create trivial tests solely to increase the percentage, nor does it enforce an arbitrary coverage threshold as part of the exercise.

Business rules, persistence behavior, API behavior and error scenarios are prioritized.

---

# OpenAPI

The REST contract is documented using OpenAPI 3:

```text
src/main/resources/static/openapi.yml
```

It documents:

- Endpoint.
- Request parameters.
- Validation constraints.
- Success response.
- Error responses.
- Examples.

`applicationDate` is represented as an ISO-8601 local date-time without timezone offset because the exercise models the date using `LocalDateTime`.

---

# Running the Application

## Requirement

```text
Java 21
```

The Maven Wrapper is included, so installing Maven separately is not required.

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

The API is available at:

```text
http://localhost:8080
```

---

# Running the Complete Verification

### Windows

```powershell
.\mvnw.cmd clean verify
```

### Linux / macOS

```bash
./mvnw clean verify
```

This executes the complete test suite and generates the JaCoCo coverage report.

---

# Project Structure

```text
com.erikj.pricing
│
├── domain
│   └── model
│       └── Price
│
├── application
│   ├── exception
│   │   └── PriceNotFoundException
│   ├── port
│   │   ├── in
│   │   │   └── GetApplicablePriceUseCase
│   │   └── out
│   │       └── LoadApplicablePricePort
│   └── service
│       └── GetApplicablePriceService
│
├── infrastructure
│   ├── adapter
│   │   ├── in
│   │   │   └── rest
│   │   │       ├── PriceController
│   │   │       ├── dto
│   │   │       │   └── PriceResponse
│   │   │       └── error
│   │   │           ├── ApiErrorResponse
│   │   │           └── GlobalExceptionHandler
│   │   └── out
│   │       └── persistence
│   │           ├── PriceJpaEntity
│   │           ├── PricePersistenceAdapter
│   │           ├── PricePersistenceMapper
│   │           └── SpringDataPriceRepository
│   └── config
│       └── ApplicationConfig
│
└── PricingServiceApplication
```

---

# Design Decisions and Trade-offs

## Hexagonal Architecture

Hexagonal Architecture introduces several small classes in this exercise, but it provides explicit boundaries between business logic and technical infrastructure.

The implementation intentionally avoids introducing additional abstractions without a concrete need.

## Pragmatic Domain Model

The project has an explicit domain model but does not introduce artificial concepts such as:

```text
AggregateRoot
Money
BrandId
ProductId
PriceListId
DomainService
```

The current business problem does not justify that complexity.

## Database-side Resolution

Price filtering, priority ordering and limitation are delegated to the database.

This minimizes data transfer and application-side processing.

## Separate Persistence Model

`PriceJpaEntity` is deliberately separated from `Price`.

This prevents JPA from becoming part of the domain model and allows persistence to evolve independently.

## LocalDateTime

The exercise supplies dates without timezone or offset information.

For that reason the application consistently uses:

```text
LocalDateTime
```

In a distributed production system where timezone semantics are relevant, `OffsetDateTime` or `Instant` would be evaluated according to business requirements.

## H2

H2 is explicitly appropriate for this exercise because it provides a real relational database while requiring no external infrastructure.

A production relational database could replace it behind the same output port.