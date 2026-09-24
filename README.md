# Pricing Service

Spring Boot REST API that resolves the applicable price for a product and brand at a given date.

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- H2
- Maven
- JUnit 5
- Mockito

## Architecture

The application follows Hexagonal Architecture principles, keeping the domain and application layers independent from infrastructure concerns.

```text
REST Adapter
     |
     v
Input Port
     |
     v
Application Service
     |
     v
Output Port
     |
     v
Persistence Adapter
     |
     v
H2 / JPA
```

Main layers:

- `domain`: domain model and business concepts.
- `application`: use cases and input/output ports.
- `infrastructure`: REST adapters, persistence adapters and Spring configuration.

The domain and application layers do not depend on Spring, JPA or HTTP.

## API

### Get applicable price

```http
GET /api/v1/prices
```

### Query parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `applicationDate` | ISO-8601 datetime | Yes | Date and time used to resolve the applicable price |
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

## Price Resolution

A price is applicable when:

```text
brandId = requested brand
productId = requested product
startDate <= applicationDate
endDate >= applicationDate
```

If multiple prices are applicable at the same time, the price with the highest `priority` is returned.

The filtering, ordering by priority and selection of a single result are performed at database level to avoid loading unnecessary records into memory.

## Database

The application uses an in-memory H2 database.

The database schema and sample data are initialized automatically at application startup using:

```text
src/main/resources/schema.sql
src/main/resources/data.sql
```

The sample dataset contains the prices specified in the technical exercise.

## Running the Application

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

The service will be available at:

```text
http://localhost:8080
```

## Running Tests

### Windows

```powershell
.\mvnw.cmd clean test
```

### Linux / macOS

```bash
./mvnw clean test
```

The project includes:

- Unit tests for the application use case using JUnit and Mockito.
- Persistence tests for the applicable-price query.
- Integration tests covering the five scenarios required by the exercise.

## Error Handling

The API returns:

- `200 OK` when an applicable price is found.
- `400 Bad Request` when request parameters are invalid.
- `404 Not Found` when no applicable price exists for the requested criteria.

Errors are returned using a consistent JSON response structure.

## OpenAPI

The API contract is documented using OpenAPI:

```text
src/main/resources/static/openapi.yml
```

It documents the endpoint, query parameters, response model and error responses.

## Project Structure

```text
com.erikj.pricing
|
├── domain
|   └── model
|
├── application
|   ├── exception
|   ├── port
|   |   ├── in
|   |   └── out
|   └── service
|
└── infrastructure
    ├── adapter
    |   ├── in
    |   |   └── rest
    |   └── out
    |       └── persistence
    └── config
```

## Design Decisions

- Hexagonal Architecture separates business logic from technical details.
- The domain model does not depend on JPA or Spring.
- Input and output ports define the boundaries of the application.
- Persistence models are separated from domain models.
- Constructor injection is used for dependencies.
- The database performs filtering, priority ordering and single-result selection.
- API DTOs are separated from domain objects.