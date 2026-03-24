# Revenu API

B2B SaaS API for dynamic recipe costing. Food entrepreneurs register ingredients, track prices over time, create recipes, and get real-time cost and margin calculations.

## Stack

- **Runtime:** Clojure (JVM)
- **HTTP:** Pedestal + Jetty
- **Database:** PostgreSQL
- **SQL:** next.jdbc + HoneySQL
- **Validation:** Prismatic Schema
- **Auth:** Google OAuth + JWT (Buddy)
- **Migrations:** Ragtime

## Getting Started

### Prerequisites

- Docker

### Setup

1. Clone the repo and copy the env template:

```bash
cp .env.example .env
```

2. Fill in your credentials in `.env`:
   - Google OAuth client ID and secret from [Google Cloud Console](https://console.cloud.google.com)
   - A JWT secret (at least 32 chars: `openssl rand -base64 32`)

3. Start the stack:

```bash
docker compose up --build
```

Migrations run automatically on startup. The API will be available at `http://localhost:8080`.

### Development

```bash
cp .env.example .env.dev  # fill in dev credentials
docker compose -f docker-compose.dev.yml up --build
```

Connect your editor to the nREPL server at `localhost:7888`.

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
| GET | `/ingredients/:id/prices` | List price history (newest first) |

`base_unit` accepts `g`, `ml`, or `unit` — this is the base unit stored for the ingredient and cannot be changed after creation.

```json
POST /ingredients
{ "name": "Farinha", "base_unit": "g" }

→ { "id": "uuid", "name": "Farinha", "base_unit": "g" }
```

```json
POST /ingredients/:id/prices
{ "price": 20.00, "package_quantity": 2, "package_unit": "kg", "purchase_date": "2026-03-10" }
```

### Recipes

| Method | Path | Description |
|--------|------|-------------|
| GET | `/recipes` | List all recipes |
| POST | `/recipes` | Create recipe |
| GET | `/recipes/:id` | Get recipe with ingredients |
| PATCH | `/recipes/:id` | Update recipe |
| DELETE | `/recipes/:id` | Soft-delete recipe |

`target_margin` is a percentage (e.g. `50.0` = 50%). Stored internally as basis points.

```json
POST /recipes
{
  "name": "Bolo de Chocolate",
  "servings": 12,
  "target_margin": 50.0,
  "ingredients": [
    { "id": "uuid", "usage_quantity": 500, "usage_unit": "g" }
  ]
}
```

All endpoints except `/auth/*` require `Authorization: Bearer <token>`.

## Architecture

Each layer is a folder, each domain is a file inside that folder. Layers have strict contracts and never cross each other.

```
src/revenu_api/
├── wire/
│   ├── in/         ← request validation schemas (Prismatic Schema)
│   ├── out/        ← response schemas
│   └── db/         ← raw DB row schemas (namespaced keys)
├── adapters/       ← data conversion between layers
├── controllers/    ← orchestration (no business logic)
├── logic/          ← pure functions (unit conversion, cost calculation)
├── db/             ← SQL queries (always filtered by user_id)
├── models/         ← internal domain schemas (source of truth)
├── interceptors.clj
├── infra/          ← HTTP server, OAuth client
└── config.clj
```

See [`docs/architecture.md`](docs/architecture.md) for full details.

## License

Copyright © 2026 João Santos
