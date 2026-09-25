# Pricing Service

REST API developed with Java and Spring Boot to resolve the applicable price for a product and brand at a specific date.

The project has been designed using **Hexagonal Architecture (Ports and Adapters)**, keeping business and application logic independent from frameworks, persistence and HTTP concerns.

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

If multiple prices are applicable at the same time, the price with the **highest priority** must be returned.

The selection is performed directly at database level so that only the required record is retrieved.

---

## Technologies

| Technology | Purpose |
|---|---|
| Java 21 | Main programming language |
| Spring Boot 4.1.1 | Application bootstrap and infrastructure configuration |
| Spring Web MVC | REST API implementation |
| Spring Data JPA | Persistence abstraction and database access |
| Hibernate | JPA implementation |
| H2 | In-memory database used for the technical exercise |
| Bean Validation | Validation of HTTP input parameters |
| Maven | Dependency management, build and test lifecycle |
| JUnit / JUnit Jupiter | Unit and integration testing |
| Mockito | Isolation of dependencies in unit tests |
| AssertJ | Fluent assertions in unit and persistence tests |
| MockMvc | HTTP integration testing without starting an external server |
| OpenAPI 3 | REST contract documentation |
| JaCoCo | Test coverage reporting |

H2 is used because the exercise explicitly requires an in-memory database and allows the application to start with a deterministic dataset without external infrastructure.

Spring Data JPA is used to keep database access inside the persistence adapter while allowing the application core to remain independent from persistence technology.

---

## Architecture

The application follows **Hexagonal Architecture**, also known as **Ports and Adapters**.

The main objective is to isolate the application core from external technologies such as HTTP, Spring Data JPA and H2.

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

### Domain

```text
domain
└── model
    └── Price
```

The domain contains the business model.

`Price` represents a price independently from:

- HTTP
- Spring
- JPA
- H2

It also protects basic domain invariants such as mandatory fields and ensuring that `startDate` cannot be after `endDate`.

---

### Application

```text
application
├── exception
├── port
│   ├── in
│   └── out
└── service
```

The application layer contains the use case and its boundaries.

#### Input port

```text
GetApplicablePriceUseCase
```

Defines what the application allows external adapters to execute.

The REST controller depends on this interface rather than directly depending on the implementation.

#### Application service

```text
GetApplicablePriceService
```

Implements the use case.

It contains the orchestration required to resolve an applicable price and depends only on the output port.

It does not know whether prices are stored in H2, PostgreSQL, MongoDB or obtained from another service.

#### Output port

```text
LoadApplicablePricePort
```

Defines what the application requires from persistence.

The application service depends on this interface instead of Spring Data JPA.

---

### Infrastructure

```text
infrastructure
├── adapter
│   ├── in
│   │   └── rest
│   └── out
│       └── persistence
└── config
```

Infrastructure contains framework-specific implementations.

#### REST adapter

```text
PriceController
PriceResponse
GlobalExceptionHandler
ApiErrorResponse
```

Responsibilities:

- Receive HTTP requests.
- Validate request parameters.
- Execute the application use case.
- Convert the domain response into an API DTO.
- Translate application errors into HTTP responses.

#### Persistence adapter

```text
PricePersistenceAdapter
SpringDataPriceRepository
PriceJpaEntity
PricePersistenceMapper
```

Responsibilities:

- Implement the application's persistence port.
- Execute the database query.
- Map persistence entities to domain objects.
- Keep JPA completely outside the domain and application layers.

---

## Dependency Direction

One of the main architectural decisions is that dependencies always point toward the application core.

```text
Controller
    |
    v
Input Port
    |
    v
Application Service
    |
    v
Output Port
    ^
    |
Persistence Adapter
```

The application layer never imports:

```text
Spring Data JPA
Hibernate
H2
HTTP
Spring MVC
```

This means infrastructure can be replaced without modifying the business use case.

For example, H2 could be replaced by PostgreSQL while keeping:

```text
GetApplicablePriceUseCase
GetApplicablePriceService
LoadApplicablePricePort
Price
```

unchanged.

---

## Model Separation

The project deliberately separates the domain, HTTP and persistence representations.

### Domain

```text
Price
```

Represents the business concept.

### REST response

```text
PriceResponse
```

Defines the information exposed through the API.

### Persistence

```text
PriceJpaEntity
```

Represents the relational database model and contains JPA annotations.

The transformation:

```text
PriceJpaEntity
        |
        v
PricePersistenceMapper
        |
        v
Price
```

prevents persistence concerns from leaking into the domain model.

The API similarly maps:

```text
Price
  |
  v
PriceResponse
```

This allows the three models to evolve independently.

---

## Request Flow

A complete request follows this path:

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

The persistence result then travels back through the application and is converted into `PriceResponse`.

---

## Price Resolution Strategy

The database query applies all relevant conditions:

```text
brandId = ?
productId = ?
startDate <= applicationDate
endDate >= applicationDate
```

When more than one price is applicable:

```text
ORDER BY priority DESC
```

is applied.

Only the first result is returned.

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

This was deliberately implemented at database level instead of:

```text
loading multiple rows
→ filtering in Java
→ sorting in Java
→ selecting the first one
```

which would perform unnecessary work in application memory.

---

## API

### Get applicable price

```http
GET /api/v1/prices
```

### Query parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `applicationDate` | ISO-8601 local date-time | Yes | Date used to resolve the price |
| `productId` | Long | Yes | Product identifier |
| `brandId` | Long | Yes | Brand identifier |

Example:

```http
GET /api/v1/prices?applicationDate=2020-06-14T16:00:00&productId=35455&brandId=1
```

Example response:

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

## Error Handling

The API uses a consistent error representation.

Possible responses include:

```text
200 OK
```

Applicable price found.

```text
400 Bad Request
```

Invalid parameters, invalid date format or missing required parameters.

```text
404 Not Found
```

No applicable price exists for the requested product, brand and date.

Example error response:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "No applicable price found...",
  "timestamp": "2020-06-14T16:00:00"
}
```

---

## Database

The application uses an in-memory H2 database.

Spring Boot automatically configures the embedded database available on the classpath.

The database schema is created from:

```text
src/main/resources/schema.sql
```

and initial data is loaded from:

```text
src/main/resources/data.sql
```

The dataset contains the four price records defined by the exercise.

Hibernate schema generation is disabled because the schema is explicitly controlled through `schema.sql`.

---

## Testing Strategy

The project separates tests according to their responsibility.

### Domain tests

```text
PriceTest
```

Verify domain invariants independently from Spring.

### Application unit tests

```text
GetApplicablePriceServiceTest
```

Use Mockito to isolate:

```text
LoadApplicablePricePort
```

and test the application use case without loading Spring or a database.

They verify:

- Returning an existing applicable price.
- Throwing `PriceNotFoundException` when no price exists.

### Persistence tests

```text
SpringDataPriceRepositoryTest
```

Use an embedded database to verify the actual persistence query.

They cover:

- Highest priority selection.
- No applicable price.
- Inclusive `startDate`.
- Inclusive `endDate`.
- Brand isolation.
- Product isolation.

### Integration tests

```text
PriceControllerIntegrationTest
```

Use:

```java
@SpringBootTest
@AutoConfigureMockMvc
```

and exercise the complete flow:

```text
HTTP
→ Controller
→ Application
→ Persistence Adapter
→ JPA
→ H2
```

The five scenarios explicitly required by the exercise are included.

Additional integration tests verify invalid input and error responses.

---

## Test Coverage

JaCoCo is used to generate a test coverage report.

Run:

```bash
./mvnw clean verify
```

or on Windows:

```powershell
.\mvnw.cmd clean verify
```

The generated report is available at:

```text
target/site/jacoco/index.html
```

Coverage is used as a diagnostic tool rather than as a target by itself. Tests are focused on meaningful behavior instead of adding tests solely to increase a percentage.

---

## OpenAPI

The REST contract is documented using OpenAPI 3.

```text
src/main/resources/static/openapi.yml
```

It contains:

- Endpoint definition.
- Request parameters.
- Validation constraints.
- Response schema.
- Error responses.
- Request and response examples.

The `applicationDate` field is documented as an ISO-8601 local date-time without timezone offset because the exercise models dates using `LocalDateTime`.

---

## Running the Application

### Requirements

Only a compatible JDK is required.

```text
Java 21+
```

The repository includes the Maven Wrapper, so a local Maven installation is not required.

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

The service starts at:

```text
http://localhost:8080
```

---

## Running Tests

### Windows

```powershell
.\mvnw.cmd clean verify
```

### Linux / macOS

```bash
./mvnw clean verify
```

---

## Project Structure

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

## Design Decisions and Trade-offs

### Hexagonal Architecture

Hexagonal Architecture was selected to keep the business use case independent from frameworks and infrastructure.

For such a small exercise this introduces some additional classes, but it demonstrates clear boundaries without introducing unnecessary domain abstractions.

### Pragmatic domain model

The project uses an explicit domain model but does not introduce artificial abstractions such as:

```text
AggregateRoot
BrandId
ProductId
Money
DomainService
```

because the current problem does not justify that complexity.

### Database-side price resolution

Filtering and priority selection are delegated to the database because this avoids loading unnecessary records into memory.

### Separate persistence model

`PriceJpaEntity` is intentionally not used as the domain model. This prevents JPA annotations and persistence details from becoming part of the application core.

### LocalDateTime

The exercise provides dates without timezone information, so `LocalDateTime` is used consistently.

For a distributed production system where timezone semantics matter, `OffsetDateTime` or `Instant` could be considered depending on the business requirements.

### H2

H2 is appropriate for this exercise because it is explicitly requested and allows deterministic execution without external dependencies.

In a production system the persistence adapter could be backed by another relational database without modifying the application ports or domain model.