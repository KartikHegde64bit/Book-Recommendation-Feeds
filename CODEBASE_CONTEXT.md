# Codebase Context — Book Recommendation Engine

> **Auto-generated context dump for LLM / Copilot consumption.**
> Last updated: 2026-02-23

---

## 1. Project Overview

A **Spring Boot 3.5.6 (Java 17)** REST API that recommends books using **pgvector**-powered semantic similarity search against a PostgreSQL database. Users sign up, log in (JWT-based stateless auth), and query for book recommendations. A companion **Python pipeline** generates vector embeddings from a Project Gutenberg catalogue CSV and stores them in PostgreSQL.

### High-Level Architecture

```
┌──────────────┐  HTTP/JSON   ┌──────────────────────────────────┐
│  Client App  │ ─────────── │  Spring Boot REST API (Java 17)  │
└──────────────┘              │  ├─ Auth (JWT / BCrypt)          │
                              │  ├─ User CRUD                    │
                              │  └─ Recommendation endpoint      │
                              └────────────┬─────────────────────┘
                                           │ JDBC + pgvector
                              ┌────────────▼─────────────────────┐
                              │  PostgreSQL 18+ with pgvector     │
                              │  ├─ app_user table                │
                              │  └─ books table (384-dim vectors) │
                              └──────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  Python Offline Pipeline (sentence-transformers)                 │
│  ├─ embed_book_token.py   → Bulk CSV import & embedding insert  │
│  └─ embedding_microservice.py → Flask /embed endpoint           │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack

| Layer | Technology | Version / Notes |
|-------|-----------|-----------------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.5.6 |
| Build tool | Maven | 3.8+ (uses Maven Wrapper `mvnw`) |
| Database | PostgreSQL + pgvector | 18+ / pgvector 0.5.0+ |
| ORM | Spring Data JPA / Hibernate | via `spring-boot-starter-data-jpa` |
| Auth | JWT (HMAC-512) | `com.auth0:java-jwt:4.5.0` |
| Security | Spring Security | Stateless, BCrypt passwords |
| Validation | Jakarta Bean Validation | `spring-boot-starter-validation` |
| Boilerplate | Lombok | `@Data`, `@Getter`, `@Setter`, etc. |
| Python | 3.12.3 | Embedding pipeline |
| Embedding model | `all-MiniLM-L6-v2` | 384-dimensional vectors via `sentence-transformers` |
| Python web | Flask | Microservice for on-demand embeddings |

---

## 3. Directory Structure & File Responsibilities

### 3.1 Root (`Book-Recommendation-Feeds/`)

| File | Purpose |
|------|---------|
| [pom.xml](pom.xml) | Maven project descriptor — defines all dependencies, plugins, and Java 17 target. |
| [README.md](README.md) | Dependency reference sheet (not a full README). |
| [mvnw](mvnw) / [mvnw.cmd](mvnw.cmd) | Maven Wrapper scripts for building without a global Maven install. |

### 3.2 `src/main/java/com/avidreader/` — Primary Application

#### Entry Point

| File | Purpose |
|------|---------|
| [ReaderRecommendationEngineApplication.java](src/main/java/com/avidreader/ReaderRecommendationEngineApplication.java) | `@SpringBootApplication` main class. Bootstraps the Spring context. |

#### Config (`config/`)

| File | Purpose |
|------|---------|
| [SecurityConfig.java](src/main/java/com/avidreader/config/SecurityConfig.java) | Spring Security configuration. Disables CSRF, form login, HTTP Basic, and sessions (stateless). Permits `/api/auth/login` and `/api/auth/signup` unauthenticated; all other endpoints require a valid JWT. Registers `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`. Exposes `AuthenticationManager` and `BCryptPasswordEncoder` beans. |
| [JwtProperties.java](src/main/java/com/avidreader/config/JwtProperties.java) | **Placeholder / empty class.** Intended for externalising JWT configuration properties (secret, expiry, issuer) but currently unused. |

#### Controllers (`controllers/`)

| File | Purpose |
|------|---------|
| [UserController.java](src/main/java/com/avidreader/controllers/UserController.java) | Base mapping `/api/users`. Contains a **nested inner class `AuthController`** mapped to `/api/auth` that exposes: |
| | `POST /api/auth/signup` — Accepts `UserDTO`, delegates to `UserService.registerNewUser()`, returns `201 Created`. |
| | `POST /api/auth/login` — Accepts `LoginRequest`, calls `UserService.login()`, generates JWT via `JwtTokenService.generateToken()`, returns `{ access_token, token_type }`. |
| [RecommendationController.java](src/main/java/com/avidreader/controllers/RecommendationController.java) | Mapped to `/api/recommend`. **Stub / work-in-progress.** Constructor accepts `JdbcTemplate` and `EmbeddingService` (not yet wired). `GET /api/recommend?query=...` is declared but the body is empty. Intended to call `RecommendationService.recommend()`. |

#### DTOs (`dtos/`)

| File | Purpose | Fields |
|------|---------|--------|
| [UserDTO.java](src/main/java/com/avidreader/dtos/UserDTO.java) | Registration request body. `@Data` (Lombok). | `username` (3-50 chars), `email` (valid email), `password` (min 8 chars). All `@NotBlank`. |
| [LoginRequest.java](src/main/java/com/avidreader/dtos/LoginRequest.java) | Login request body. `@Data` (Lombok). | `username` (3-50 chars), `password` (min 8 chars). All `@NotBlank`. |
| [AuthResponse.java](src/main/java/com/avidreader/dtos/AuthResponse.java) | JWT response. Java **record**. | `access_token`, `token_type`. (Currently unused — controller returns a `Map` instead.) |

#### Entity (`entity/`)

| File | Purpose |
|------|---------|
| [User.java](src/main/java/com/avidreader/entity/User.java) | JPA `@Entity` mapped to table `app_user`. Fields: `id` (auto-increment), `username` (unique, max 50), `email` (unique, max 255), `passwordHash`, `createdAt` (`@CreationTimestamp`), `updatedAt` (`@UpdateTimestamp`). Provides `setPassword(raw, encoder)` and `checkPassword(raw, encoder)` helper methods using BCrypt. Uses Lombok `@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor`. |

#### Repository (`repository/`)

| File | Purpose |
|------|---------|
| [UserRepository.java](src/main/java/com/avidreader/repository/UserRepository.java) | Spring Data JPA repository for `User`. Derived query methods: `findByUsername(String)`, `findByEmail(String)`. Both return `Optional<User>`. |

#### Security (`security/`)

| File | Purpose |
|------|---------|
| [JwtAuthFilter.java](src/main/java/com/avidreader/security/JwtAuthFilter.java) | `OncePerRequestFilter` `@Component`. Skips filtering for `/api/auth/signup` and `/api/auth/login`. For all other requests, extracts `Bearer` token from `Authorization` header, delegates to `JwtTokenService.parseAndBuildAuthentication()` to validate and build a Spring `Authentication` object, then sets it in `SecurityContextHolder`. |
| [JwtTokenService.java](src/main/java/com/avidreader/security/JwtTokenService.java) | `@Service`. Uses **HMAC-512** algorithm with secret read from `JWT_SECRET` env var (must be ≥ 64 chars). Provides: |
| | `generate(UserDetails)` — Creates JWT with subject, issued-at, 1-hour expiry, and roles claim from the UserDetails authorities. |
| | `generateToken(String username)` — Simplified overload for signup; assigns default `ROLE_USER`. |
| | `parseAndBuildAuthentication(String token)` — Verifies signature + expiry, extracts subject & roles, returns `UsernamePasswordAuthenticationToken`. |

#### Services (`services/`)

| File | Purpose |
|------|---------|
| [UserService.java](src/main/java/com/avidreader/services/UserService.java) | `@Service`. Injects `UserRepository`, `PasswordEncoder`, `AuthenticationManager`, `JwtTokenService`. Key methods: |
| | `registerNewUser(UserDTO)` — Checks username/email uniqueness, hashes password, persists `User`. |
| | `login(username, rawPassword)` — Looks up user by username and returns it. **Note:** does not currently verify the password — likely a WIP. |
| | `authenticateUser(username, rawPassword)` — Legacy method that manually verifies password via `User.checkPassword()`. Returns `Optional<User>`. |
| | `findById(Long)`, `deleteUser(Long)` — Basic CRUD delegates. |
| [RecommendationService.java](src/main/java/com/avidreader/services/RecommendationService.java) | **Not annotated as `@Service` yet.** Accepts `JdbcTemplate` and an `EmbeddingService` (unresolved — class doesn't exist in codebase). `recommend(String query)` converts query to a 384-dim vector, then runs a pgvector nearest-neighbour SQL (`embedding <#> ?::vector`) against the `books` table, returning top-5 results with id, title, authors, bookshelves, and distance. |

### 3.3 `src/main/resources/`

| File | Purpose |
|------|---------|
| [application.properties](src/main/resources/application.properties) | App name, PostgreSQL JDBC URL (`localhost:5432/book_recommendation`), datasource credentials. Hibernate DDL-auto and dialect lines are commented out. |

### 3.4 `src/test/`

| File | Purpose |
|------|---------|
| [ReaderRecommendationEngineApplicationTests.java](src/test/java/com/avidreader/Application/ReaderRecommendationEngineApplicationTests.java) | Single `@SpringBootTest` with a `contextLoads()` smoke test. |

### 3.5 `python/` — Embedding Pipeline

| File | Purpose |
|------|---------|
| [embed_book_token.py.txt](python/embed_book_token.py.txt) | **Offline batch script.** Reads a CSV (`books.csv`), creates the `books` table in PostgreSQL (with a `vector(384)` column for pgvector), iterates over rows, generates embeddings using `sentence-transformers` (`all-MiniLM-L6-v2`), and bulk-inserts into the DB. Columns: `id`, `type`, `issued`, `title`, `language`, `authors`, `subjects`, `locc`, `bookshelves`, `embedding`, `created_at`. |
| [embedding_microservice.py.txt](python/embedding_microservice.py.txt) | **Flask microservice.** Exposes `POST /embed` accepting `{ "text": "..." }`, returns `{ "embedding": [...] }` using the same `all-MiniLM-L6-v2` model. Runs on `http://127.0.0.1:5000`. The Spring Boot app's `RecommendationService` is expected to call this service (via an `EmbeddingService` client that has **not yet been implemented**). |

### 3.6 `TrainingData/`

| File | Purpose |
|------|---------|
| [pg_catalog.csv](TrainingData/pg_catalog.csv) | **~88,270 rows** of Project Gutenberg book metadata. Columns: `Text#`, `Type`, `Issued`, `Title`, `Language`, `Authors`, `Subjects`, `LoCC`, `Bookshelves`. This is the source data fed into `embed_book_token.py`. |

### 3.7 `demo/` — Scaffold / Skeleton

A minimal Spring Boot 3.5.6 project generated by Spring Initializr. Contains only `spring-boot-starter` (no web, no JPA). Has its own `pom.xml`, `mvnw`, and a bare `ReaderRecommendationEngineApplication.java`. **Not part of the main application — likely the initial scaffold before the real project was built.**

---

## 4. Database Schema (Inferred)

### `app_user` (managed by JPA / Hibernate)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGSERIAL` | PK, auto-increment |
| `username` | `VARCHAR(50)` | NOT NULL, UNIQUE |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `password_hash` | `VARCHAR(255)` | NOT NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, auto-set |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, auto-updated |

### `books` (created by Python script)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGSERIAL` | PK |
| `type` | `TEXT` | |
| `issued` | `TEXT` | |
| `title` | `TEXT` | |
| `language` | `TEXT` | |
| `authors` | `TEXT` | |
| `subjects` | `TEXT` | |
| `locc` | `TEXT` | |
| `bookshelves` | `TEXT` | |
| `embedding` | `vector(384)` | pgvector type |
| `created_at` | `TIMESTAMPTZ` | DEFAULT `now()` |

---

## 5. API Endpoints

| Method | Path | Auth | Request Body | Response | Status |
|--------|------|------|-------------|----------|--------|
| `POST` | `/api/auth/signup` | Public | `UserDTO` (`username`, `email`, `password`) | `{ "message": "created successfully" }` | `201` |
| `POST` | `/api/auth/login` | Public | `LoginRequest` (`username`, `password`) | `{ "access_token": "...", "token_type": "Bearer" }` | `200` |
| `GET` | `/api/recommend?query=...` | Bearer JWT | — | List of top-5 book matches (id, title, authors, bookshelves, distance) | **WIP** |

---

## 6. Authentication Flow

1. **Signup:** Client → `POST /api/auth/signup` with JSON body → `UserService.registerNewUser()` hashes password with BCrypt → persists to `app_user`.
2. **Login:** Client → `POST /api/auth/login` → `UserService.login()` retrieves user → `JwtTokenService.generateToken(username)` creates HMAC-512 JWT with `ROLE_USER` and 1-hour expiry → returned to client.
3. **Authenticated requests:** Client sends `Authorization: Bearer <token>` → `JwtAuthFilter` intercepts → `JwtTokenService.parseAndBuildAuthentication()` verifies → sets `SecurityContextHolder` → request proceeds.

---

## 7. Recommendation Flow (Intended)

1. Client sends `GET /api/recommend?query=science+fiction+space` with JWT.
2. `RecommendationController` delegates to `RecommendationService.recommend(query)`.
3. `RecommendationService` calls an `EmbeddingService` (planned HTTP client to the Python Flask microservice at `localhost:5000/embed`) to get a 384-dim vector for the query.
4. Runs pgvector nearest-neighbour search: `SELECT ... FROM books ORDER BY embedding <#> ?::vector LIMIT 5`.
5. Returns top-5 books ranked by cosine distance.

---

## 8. Known Gaps / Work-in-Progress

| Area | Detail |
|------|--------|
| `EmbeddingService` | Referenced in `RecommendationService` and `RecommendationController` but **class does not exist**. Needs to be created as an HTTP client calling the Python Flask `/embed` endpoint. |
| `RecommendationController` | Method body is empty — needs to delegate to `RecommendationService`. |
| `RecommendationService` | Missing `@Service` annotation; won't be picked up by component scan. Also references non-existent `EmbeddingService`. |
| `UserService.login()` | Does **not verify password** — it only fetches the user by username. Should call `checkPassword()` or `authenticateUser()` before returning. |
| `AuthResponse` record | Defined but unused — controller builds response with `Map.of()` instead. |
| `JwtProperties` | Empty class — JWT secret is read from env var directly in `JwtTokenService`. Could be wired via `@ConfigurationProperties`. |
| `UserController` nesting | `AuthController` is a **nested inner class** inside `UserController`. This works but is unconventional — could be extracted to a standalone controller. |
| Python scripts | Saved as `.py.txt` files — need renaming to `.py` to be executable. Template placeholders (`{model_name}`, `{postgres_username}`, etc.) in `embed_book_token.py.txt` need to be filled in. |
| Tests | Only a single `contextLoads()` test exists. No unit or integration tests for services, controllers, or security. |
| `demo/` directory | Vestigial scaffold — can be removed to avoid confusion. |

---

## 9. Environment Variables

| Variable | Required By | Description |
|----------|------------|-------------|
| `JWT_SECRET` | `JwtTokenService` | HMAC-512 signing key. Must be ≥ 64 characters. App fails to start if missing or too short. |

---

## 10. Build & Run

```bash
# Build
./mvnw clean package -DskipTests

# Run (ensure PostgreSQL is running and JWT_SECRET is set)
JWT_SECRET="your-64-char-or-longer-secret-key-here..." \
  java -jar target/Project-0.0.1-SNAPSHOT.jar

# Python: bulk embed books
cd python
python embed_book_token.py  # (after renaming from .py.txt and filling in placeholders)

# Python: start embedding microservice
python embedding_microservice.py  # serves on http://127.0.0.1:5000
```

---

## 11. Key Conventions

- **Package root:** `com.avidreader`
- **Lombok** is used for boilerplate (DTOs use `@Data`, entity uses `@Getter/@Setter`).
- **Java records** are used for immutable DTOs (`AuthResponse`).
- **Jakarta EE** namespace (`jakarta.persistence`, `jakarta.servlet`, `jakarta.validation`).
- **Constructor injection** throughout (no `@Autowired` annotations).
- Passwords are **BCrypt-hashed** at the entity level via `User.setPassword(raw, encoder)`.
- JWT tokens carry a `roles` claim; default role is `ROLE_USER`.
- Database column naming follows **snake_case**; Java fields follow **camelCase**.
