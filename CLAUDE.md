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
| Tests | Kaocha |
| CI/CD | GitHub Actions |

## Architecture: Diplomat Pattern (layer-grouped)

Each layer is a **folder**. Each domain is a **file inside that folder**.
Layers have strict contracts — never cross them.

```
src/revenu/
├── wire/in/        ← request validation + unit conversion (kg→g, reais→cents)
├── wire/out/       ← response formatting (cents→reais, bps→%)
├── controller/     ← orchestrates flow: calls db + logic, never business logic
├── logic/          ← pure functions only, no DB or HTTP
├── db/             ← SQL via next.jdbc, every query filtered by user_id
├── model/          ← Prismatic Schema definitions (source of truth for types)
├── middleware/     ← Pedestal interceptors: JWT auth, rate limiting, RFC 7807 errors
├── routes.clj
├── config.clj
├── db_core.clj
└── system.clj
```

See [`docs/architecture.md`](docs/architecture.md) for layer contracts, data
flow, and business rules.

## Critical Rules (read before touching anything)

**Money:** always stored as integer **cents**. Never floats.  
**Quantities:** always stored as integer **base units** (g, ml, unit). Never kg/l.  
**Margins:** always stored as integer **basis points** (5000 = 50%). Never percentages.  
**Conversions:** happen **only** in `wire/in` (inbound) and `wire/out` (outbound).
The `logic/` and `db/` layers only ever see integers.

**Tenant isolation:** every `db/` query **must** filter by `user_id`. No exceptions.  
**PRICE_HISTORY:** append-only. No UPDATE or DELETE endpoints exist. Ever.  
**Soft-delete:** ingredients and recipes use `is_active = false`. Hard deletes are forbidden.

## Database Schema (5 tables)

```
USERS               id, external_provider, external_id, name, email, timestamps
INGREDIENTS         id, user_id, name, unit (g|ml|unit), is_active, timestamps
PRICE_HISTORY       id, ingredient_id, price_cents, package_qty, purchase_date, created_at
RECIPES             id, user_id, name, yield_portions, target_margin_bps, is_active, timestamps
RECIPE_INGREDIENTS  id, recipe_id, ingredient_id, usage_qty, created_at
```

**Latest price rule:** `ORDER BY purchase_date DESC, id DESC LIMIT 1`

## Cost Calculation (pure integers, lives in `logic/recipe.clj`)

```
cost_per_unit       = price_cents / package_qty
ingredient_cost     = cost_per_unit * usage_qty
total_recipe_cost   = sum(ingredient_cost for all ingredients)
cost_per_portion    = total_recipe_cost / yield_portions
suggested_price     = cost_per_portion * 10000 / (10000 - target_margin_bps)
```

Edge cases:
- Recipe with no ingredients → `total_cost = 0`, not an error
- Ingredient with no price history → `cost = null`, recipe total = `null`
- `target_margin_bps = 10000` → rejected with 422 (division by zero)

## Auth Flow

No passwords. Google OAuth only.

1. Client redirects user to Google
2. Google returns auth code
3. Client POSTs code to `POST /auth/callback`
4. API exchanges code with Google, upserts user by `(external_provider, external_id)`
5. API returns `access_token` (15 min) + `refresh_token` (7 days)

## API Conventions

- All endpoints require `Authorization: Bearer <token>` except `/auth/*`
- Lists use cursor-based pagination (`?cursor=`, `?limit=`, default 20, max 100)
- Errors follow RFC 7807: `{ type, title, status, detail, errors }`
- Docs available at `/api/docs` (Swagger UI, auto-generated from routes)

## Unit Conversion Map (`logic/unit_conversion.clj`)

```clojure
{"kg"    {:base "g"    :factor 1000}
 "g"     {:base "g"    :factor 1}
 "l"     {:base "ml"   :factor 1000}
 "ml"    {:base "ml"   :factor 1}
 "dozen" {:base "unit" :factor 12}
 "unit"  {:base "unit" :factor 1}}
```

## Rate Limits

- Free tier: 100 req/min
- Paid tier: 500 req/min
- Exceeded: HTTP 429