# Marketplace Project

A role-based online marketplace built with Spring Boot, Thymeleaf, Spring Security, and Spring Data JPA.

This project supports three roles:

- Admin: manage users and monitor orders
- Seller: create/manage products and update order statuses
- Buyer: browse products, add to cart, place orders, and track/cancel eligible orders

## Tech Stack

- Java 21
- Spring Boot 4.0.3
- Spring MVC + Thymeleaf
- Spring Security (form login + role-based authorization)
- Spring Data JPA (Hibernate)
- PostgreSQL (runtime)
- H2 (tests)
- Maven Wrapper
- Docker + Docker Compose

## Key Features

- Authentication and registration (Buyer/Seller)
- Role-based dashboards (Admin, Seller, Buyer)
- Product catalog with search and category filtering
- Seller product CRUD (ownership enforced)
- Session cart for buyers
- Checkout and order placement flow
- Seller order status management (guarded transitions)
- Buyer order cancellation for eligible statuses
- Automatic stock decrease on purchase and restock on cancellation
- Starter data seeding (roles, admin, seller, sample products)

## Project Structure

```text
marketplace-project/
	src/main/java/com/lab/marketplace/
		config/          # Security and startup data seeding
		controller/      # Web and REST controllers
		dto/             # Request/response DTOs
		entity/          # JPA entities
		exception/       # REST error handling
		repository/      # Spring Data JPA repositories
		security/        # UserDetailsService implementation
		service/         # Business logic
	src/main/resources/
		templates/       # Thymeleaf pages and fragments
		static/          # CSS and JS assets
		application.yaml # Main runtime config
	src/test/
		java/            # Unit tests
		integration/     # Data JPA integration tests
		resources/       # Test config
	Dockerfile
	compose.yaml
	pom.xml
```

## Domain Model

- User <-> Role: many-to-many
- Product -> User (seller): many-to-one
- Order -> User (buyer): many-to-one
- Order -> OrderItem: one-to-many
- OrderItem -> Product: many-to-one

## Security Model

Configured in `SecurityConfig`:

- Public: login/register, product browsing, static assets, public auth/product REST endpoints
- Admin: `/admin/**`
- Seller: `/seller/**`
- Buyer: `/buyer/**`, `/cart/**`, `/orders/checkout`, `/orders/place`, `/orders/confirmation`

Authentication uses Spring Security form login and session cookies.

## Data Initialization

At startup, the app seeds:

- Roles: `ADMIN`, `SELLER`, `BUYER`
- Default admin user:
	- username: `admin`
	- password: `admin123`
- Default seller user:
	- username: `seller`
	- password: `seller123`
- Starter products (if catalog has too few in-stock items)

Change default credentials before production use.

## Prerequisites

- JDK 21
- Maven (optional if using wrapper)
- PostgreSQL 14+ (or Docker)
- Git

## Environment Variables

Set these before running the app:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_SECURITY_USER_NAME`
- `SPRING_SECURITY_USER_PASSWORD`
- `JWT_SECRET`
- `SERVER_PORT` (optional, default: `8080`)

Example (PowerShell):

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/marketplace"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
$env:SPRING_SECURITY_USER_NAME="local-admin"
$env:SPRING_SECURITY_USER_PASSWORD="local-admin-password"
$env:JWT_SECRET="replace-with-a-long-random-secret"
$env:SERVER_PORT="8080"
```

## Running Locally

### Option 1: Maven Wrapper (standard)

```powershell
./mvnw.cmd clean spring-boot:run
```

### Option 2: Reliable launcher fallback (Windows shells)

If `spring-boot:run` fails due to shell parsing, run Maven through Java directly:

```powershell
java -classpath ".mvn/wrapper/maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$PWD" org.apache.maven.wrapper.MavenWrapperMain clean spring-boot:run
```

App URL:

- `http://localhost:8080`

If the port is occupied, set a different `SERVER_PORT` first.

## Running with Docker Compose

1. Create a `.env` file in project root with values for:
	 - `POSTGRES_DB`
	 - `POSTGRES_USER`
	 - `POSTGRES_PASSWORD`
	 - `SPRING_SECURITY_USER_NAME`
	 - `SPRING_SECURITY_USER_PASSWORD`
	 - `JWT_SECRET`
	 - `APP_PORT` (optional, default maps host `8082` -> container `8080`)
2. Start services:

```powershell
docker compose up --build
```

App URL (default mapping):

- `http://localhost:8082`

## Testing

Run all tests:

```powershell
./mvnw.cmd test
```

Test setup:

- Uses H2 in-memory DB from `src/test/resources/application.yaml`
- Includes unit tests for services, DTO/entity behavior, and security
- Includes integration tests for repository query behavior

## Main Web Routes

- `/login`, `/register`
- `/products`, `/products/search`, `/products/{id}`
- `/buyer/dashboard`, `/buyer/orders`, `/cart`, `/orders/checkout`
- `/seller/dashboard`, `/seller/products`, `/seller/orders`
- `/admin/dashboard`, `/admin/users`, `/admin/orders`

## Main REST Routes

Auth:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

Products:

- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/products/search?keyword=...`
- `GET /api/products/category/{category}`
- `GET /api/products/seller` (seller only)
- `POST /api/products` (seller only)
- `PUT /api/products/{id}` (seller only)
- `DELETE /api/products/{id}` (seller only)

## Build Artifacts

Maven generates output in:

- `target/`

This folder should not be committed.

## Branching and Contribution

Suggested workflow:

1. Branch from `develop`
2. Make focused changes
3. Run tests
4. Commit with clear message
5. Push and open PR to `develop`

Example:

```powershell
git checkout develop
git pull origin develop
git checkout -b feature/your-change
git add .
git commit -m "Describe your change"
git push -u origin feature/your-change
```

## Production Notes

- Enable CSRF protection (currently disabled for simplicity)
- Replace default seeded credentials
- Use secure secret management for env vars
- Tighten logging levels in production
- Consider migration-based DB versioning (Flyway/Liquibase)
