# Revenu — Claude Context

B2B SaaS API for dynamic recipe costing. Food entrepreneurs register
ingredients, track prices over time, create recipes, and get real-time
cost/margin calculations.

## Stack

| Layer | Technology |
|---|---|
| Runtime | Clojure (JVM) |
| HTTP | Pedestal + Jetty |
| Database | PostgreSQL |
| SQL | next.jdbc (raw SQL, no ORM) |
| Validation | Prismatic Schema |
| Auth | Buddy (JWT) + Google OAuth |
| Migrations | Migratus |
| Tests | clojure.test |

## Architecture: Diplomat Pattern (layer-grouped)

Each layer is a **folder**. Each domain is a **file inside that folder**.
Layers have strict contracts — never cross them.

```
src/revenu_api/
├── wire/in/        ← request validation schemas (Prismatic Schema)
├── adapters/       ← db->internal and internal->wire conversions (wire/out logic lives here)
├── controllers/    ← orchestrates flow: calls db + logic, never business logic
├── logic/          ← pure functions only, no DB or HTTP
├── db/             ← SQL via next.jdbc + HoneySQL, every query filtered by user_id
├── models/         ← Prismatic Schema definitions (source of truth for types)
├── interceptors.clj ← Pedestal interceptors: JWT auth, schema coercion
├── infra/          ← http_server.clj (routes + handlers), google.clj (OAuth), system.clj
└── config.clj
```

See [`docs/architecture.md`](docs/architecture.md) for layer contracts, data
flow, and business rules.

## Critical Rules (read before touching anything)

**Money:** always stored as integer **cents**. Never floats.  
**Quantities:** always stored as integer **base units** (g, ml, unit). Never kg/l.  
**Margins:** stored as integer **basis points** internally (5000 = 50%). API accepts/returns plain percentage (50.0). Conversion happens in wire/in (inbound) and adapters `internal->wire` (outbound).  
**Conversions:** happen **only** in `wire/in` (inbound) and `wire/out` (outbound).
The `logic/` and `db/` layers only ever see integers.

**Tenant isolation:** every `db/` query **must** filter by `user_id`. No exceptions.  
**PRICE_HISTORY:** append-only. No UPDATE or DELETE endpoints exist. Ever.  
**Soft-delete:** ingredients and recipes use `is_active = false`. Hard deletes are forbidden.

## Database Schema (5 tables)

```
USERS               id, external_provider, external_id, name, email, timestamps
INGREDIENTS         id, user_id, name, unit (g|ml|unit), is_active, timestamps
PRICE_HISTORY       id, ingredient_id, price_cents, package_size, purchase_date, created_at
RECIPES             id, user_id, name, servings, target_margin_bps, is_active, timestamps
RECIPE_INGREDIENTS  id, recipe_id, ingredient_id, quantity, created_at
REFRESH_TOKENS      id, user_id, token_hash, expires_at, created_at
```

**Latest price rule:** `ORDER BY purchase_date DESC, id DESC LIMIT 1`

## Cost Calculation (pure integers, lives in `logic/recipe.clj`)

```
cost_per_unit       = price_cents / package_size
ingredient_cost     = cost_per_unit * quantity
total_recipe_cost   = sum(ingredient_cost for all ingredients)
cost_per_portion    = total_recipe_cost / servings
suggested_price     = cost_per_portion * 10000 / (10000 - target_margin_bps)
```

Edge cases:
- Recipe with no ingredients → `total_cost = 0`, not an error
- Ingredient with no price history → `cost = null`, recipe total = `null`
- `target_margin_bps = 10000` → rejected with 422 (division by zero)

## Auth Flow

No passwords. Google OAuth only.

1. Client calls `GET /auth/login` → gets Google redirect URL
2. Client redirects user to Google
3. Google returns auth code
4. Client POSTs code to `POST /auth/callback`
5. API exchanges code with Google, upserts user by `(external_provider, external_id)`
6. API returns `access_token` (15 min) + `refresh_token` (7 days)
7. `POST /auth/refresh` exchanges a valid refresh token for a new token pair
8. `POST /auth/logout` invalidates the refresh token (stored hash deleted from DB)

## API Conventions

- All endpoints require `Authorization: Bearer <token>` except `/auth/*`
- Lists use cursor-based pagination (not yet implemented)
- Errors follow RFC 7807: `{ type, title, status, detail, errors }`
- All request/response fields use **snake_case**

## Coding Conventions

- **Controller arg order:** `[database user-id ...]`
- **Internal representation:** kebab-case keywords (`:servings`, `:target-margin-bps`)
- **Wire output:** snake_case keys (`:servings`, `:target_margin_bps`) — converted in `internal->wire`
- **Interceptor short-circuit:** always use `chain/terminate` — assigning `:response` alone does NOT stop the Pedestal chain
- **Route protection:** `(conj protected handler)` or `(conj protected (coerce Schema) handler)` where `protected = [jwt-interceptor]`
- **JSON aggregation:** recipes fetch ingredients via LEFT JOIN + `json_agg(json_build_object(...))` parsed with cheshire in the adapter
- **HoneySQL:** `:filter` special syntax does not work nested inside select alias expressions — use adapter-side filtering instead

## Unit Conversion Map (`logic/unit_conversion.clj`)

```clojure
{"kg"    {:base "g"    :factor 1000}
 "g"     {:base "g"    :factor 1}
 "l"     {:base "ml"   :factor 1000}
 "ml"    {:base "ml"   :factor 1}
 "dozen" {:base "unit" :factor 12}
 "unit"  {:base "unit" :factor 1}}
```

Conversion happens in `wire->internal` for ingredients — the API accepts `kg/l/dozen/unit/g/ml` and stores only base units internally.

## Not Yet Implemented (MVP backlog)

- Unit conversion in ingredient wire->internal (`kg→g`, `l→ml`, `dozen→unit`)
- `target_margin` percentage conversion (wire/in * 100 → bps, wire/out / 100 → %)
- `GET /recipes/:id/cost` — cost + suggested price calculation
- `POST /auth/logout` — refresh token invalidation (requires REFRESH_TOKENS table)
- Refresh token storage and validation against DB