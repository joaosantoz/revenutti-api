# Revenu API

B2B SaaS API for dynamic recipe costing. Food entrepreneurs register ingredients, track prices over time, create recipes, and get real-time cost and margin calculations.

## Stack

- **Runtime:** Clojure (JVM)
- **HTTP:** Pedestal + Jetty
- **Database:** PostgreSQL
- **SQL:** next.jdbc + HoneySQL
- **Auth:** Google OAuth + JWT (Buddy)
- **Migrations:** Migratus

## Getting Started

### Prerequisites

- Java 21+
- Leiningen
- Docker

### Setup

1. Clone the repo and copy the env template:

```bash
cp profiles.example.clj profiles.clj
```

2. Fill in your credentials in `profiles.clj`:
   - Google OAuth client ID and secret from [Google Cloud Console](https://console.cloud.google.com)
   - A JWT secret (at least 32 chars: `openssl rand -base64 32`)

3. Start the development environment (Docker + REPL):

```bash
./dev.sh
```

4. Run migrations:

```bash
lein migrate
```

The API will be available at `http://localhost:8080`.

## API

### Auth

| Method | Path | Description |
|--------|------|-------------|
| GET | `/auth/login` | Returns Google OAuth redirect URL |
| POST | `/auth/callback` | Exchanges Google code for JWT tokens |
| POST | `/auth/refresh` | Refreshes access token |

### Ingredients

| Method | Path | Description |
|--------|------|-------------|
| GET | `/ingredients` | List all ingredients |
| POST | `/ingredients` | Create ingredient |
| GET | `/ingredients/:id` | Get ingredient |
| PATCH | `/ingredients/:id` | Update ingredient |
| DELETE | `/ingredients/:id` | Soft-delete ingredient |
| POST | `/ingredients/:id/prices` | Add price entry |
| GET | `/ingredients/:id/prices` | List price history |

### Recipes

| Method | Path | Description |
|--------|------|-------------|
| GET | `/recipes` | List all recipes |
| POST | `/recipes` | Create recipe |
| GET | `/recipes/:id` | Get recipe with ingredients |
| PATCH | `/recipes/:id` | Update recipe |
| DELETE | `/recipes/:id` | Soft-delete recipe |

All endpoints except `/auth/*` require `Authorization: Bearer <token>`.

## Architecture

Each layer is a folder, each domain is a file inside that folder. Layers have strict contracts and never cross each other.

```
src/revenu_api/
├── wire/in/        ← request validation schemas
├── adapters/       ← data conversion between layers
├── controllers/    ← orchestration (no business logic)
├── logic/          ← pure functions
├── db/             ← SQL queries (always filtered by user_id)
├── models/         ← schema definitions
├── interceptors.clj
├── infra/          ← HTTP server, OAuth client
└── config.clj
```

See [`docs/architecture.md`](docs/architecture.md) for full details.

## License

Copyright © 2026 João Santos
