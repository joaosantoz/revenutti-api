# Revenu — Architecture Reference

> Deep-dive companion to `CLAUDE.md`. Covers layer contracts, request lifecycle,
> business rules, and the full API contract.

## Layer Contracts

### `wire/in/`
- Validates incoming request shape with Prismatic Schema
- Accepts user-friendly units (`kg/l/dozen`) — actual conversion happens in adapters
- **Never** accesses the database or calls logic functions

### `wire/out/`
- Defines response schemas with Prismatic Schema
- **Never** accesses the database

### `wire/db/`
- Schemas for raw DB rows as returned by next.jdbc
- Uses namespaced keys matching the table name (e.g. `:ingredients/base_unit`)
- **Never** used outside of adapters

### `adapters/`
- All conversions between layers live here
- `wire->internal`: converts wire/in map → internal model (unit→base, price→cents, margin→bps)
- `db->internal`: converts raw DB row → internal model (namespaced keys → kebab keys)
- `internal->wire`: converts internal model → wire/out map (cents→price, bps→%)
- **Never** accesses the database directly

### `controllers/`
- Orchestrates the request: calls `db/` to fetch/write data, calls `logic/` for calculations
- Validates business constraints (e.g. ingredient existence, unit compatibility)
- **Never** contains business logic or domain rules
- One file per domain: `recipe.clj`, `ingredient.clj`, `price_history.clj`, `auth.clj`

### `logic/`
- Pure functions only — no side effects, no DB access, no HTTP
- Cost calculation lives here: `logic/recipe.clj` (planned)
- Unit conversion map lives here: `logic/unit_conversion.clj`

### `db/`
- Raw SQL via `next.jdbc` + HoneySQL
- **Every single query filters by `user_id`** — this is the multi-tenant isolation boundary
- No business logic here — just fetch/insert/update/deactivate

### `models/`
- Prismatic Schema definitions for internal domain entities
- Source of truth for internal types
- `models/unit.clj` — shared unit type schemas (`BaseUnitType`, `WireUnitType`)
- **Never** include timestamps — timestamps only appear in wire/out when needed

### `interceptors.clj`
- `jwt-interceptor` — validates Bearer token, injects `user-id` UUID into request
- `coerce` — validates request body against a Prismatic Schema, returns 422 on failure
- `error-interceptor` — catches `ex-info` with `:status`, returns RFC 7807 error shape
- `db-interceptor` — injects database connection into request context

### `infra/`
- `http_server.clj` — route definitions and HTTP handlers
- `auth.clj` — JWT signing and verification (access: 15 min, refresh: 7 days)
- `google.clj` — Google OAuth HTTP calls

---

## Request Lifecycle

```
HTTP Request
  ↓
interceptors/error-interceptor  → catches all ex-info, returns RFC 7807
interceptors/jwt-interceptor    → validates JWT, injects user-id
  ↓
interceptors/coerce             → validates Prismatic schema, returns 422 on failure
  ↓
infra/http_server.clj handler   → extracts params, calls adapter + controller
  ↓
adapters/wire->internal         → converts wire input to internal model
  ↓
controllers/recipe.clj          → orchestrates:
  ├── db/ingredient.clj         → validates ingredient existence + unit compatibility
  ├── db/recipe.clj             → fetches/writes data (filtered by user_id)
  └── logic/recipe.clj          → calculates costs (pure function, planned)
  ↓
adapters/internal->wire         → converts internal model to wire output
  ↓
JSON Response
```

---

## Business Rules

| ID | Rule | Priority |
|---|---|---|
| BR-S01 | Every DB query filters by `user_id` (tenant isolation) | Critical |
| BR-N01 | Ingredient `base_unit` is always stored as base (`g/ml/unit`). Adapter converts on input. | Critical |
| BR-N02 | `usage_unit` in recipe_ingredients must be compatible with ingredient's `base_unit` | Critical |
| BR-N03 | `package_unit` in price_history must be compatible with ingredient's `base_unit` | Critical |
| BR-N04 | Recipe cost uses the latest price: `ORDER BY purchase_date DESC, id DESC LIMIT 1` | Critical |
| BR-N05 | `PRICE_HISTORY` is append-only. No UPDATE or DELETE. Ever. | Critical |
| BR-N06 | Soft-deleted ingredients remain in existing recipes for cost history. Cannot be added to new recipes. | High |
| BR-N07 | Recipe with zero ingredients returns `cost = 0`, not an error | Medium |
| BR-N08 | Ingredient with no price history returns `cost = null`, not zero | High |
| BR-N09 | `target_margin_bps = 10000` → rejected with 422 (division by zero) | High |

---

## Database Schema

### USERS
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | DEFAULT gen_random_uuid() |
| external_provider | TEXT | `"google"` |
| external_id | TEXT | Unique ID from provider |
| name | TEXT | From OAuth profile |
| email | TEXT NOT NULL | From OAuth profile |
| created_at / updated_at | TIMESTAMPTZ | DEFAULT NOW() |

Unique constraint: `(external_provider, external_id)`

### INGREDIENTS
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → USERS | Isolation key |
| name | TEXT NOT NULL | |
| base_unit | TEXT NOT NULL | `g`, `ml`, or `unit` — always stored as base |
| is_active | BOOLEAN DEFAULT TRUE | Soft-delete flag |
| created_at / updated_at | TIMESTAMPTZ | |

Check: `base_unit IN ('g', 'ml', 'unit')`

### PRICE_HISTORY
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| ingredient_id | UUID FK → INGREDIENTS | |
| price_cents | INTEGER NOT NULL | Price in cents |
| package_quantity | INTEGER NOT NULL | Quantity as entered by user |
| package_unit | TEXT NOT NULL | Unit as entered (`kg`, `g`, `l`, `ml`, `dozen`, `unit`) |
| purchase_date | DATE NOT NULL | |
| created_at | TIMESTAMPTZ | |

**Append-only. No UPDATE or DELETE endpoints.**

### RECIPES
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → USERS | Isolation key |
| name | TEXT NOT NULL | |
| servings | INTEGER NOT NULL | Number of portions the recipe yields |
| target_margin_bps | INTEGER NOT NULL DEFAULT 0 | Target margin in bps (5000 = 50%) |
| is_active | BOOLEAN DEFAULT TRUE | Soft-delete flag |
| created_at / updated_at | TIMESTAMPTZ | |

### RECIPE_INGREDIENTS
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| recipe_id | UUID FK → RECIPES | |
| ingredient_id | UUID FK → INGREDIENTS | |
| usage_quantity | INTEGER NOT NULL | Quantity as entered by user |
| usage_unit | TEXT NOT NULL | Unit as entered — must be compatible with ingredient's base_unit |
| created_at | TIMESTAMPTZ | |

Check: `usage_unit IN ('kg', 'g', 'l', 'ml', 'dozen', 'unit')`

### REFRESH_TOKENS
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → USERS | |
| token_hash | TEXT NOT NULL | Hashed refresh token |
| expires_at | TIMESTAMPTZ NOT NULL | |
| created_at | TIMESTAMPTZ | |

---

## Cost Calculation

Planned — lives in `logic/recipe.clj`. All arithmetic uses integers (cents, base units, bps).

```
package_factor     = conversion factor for package_unit → base
usage_factor       = conversion factor for usage_unit → base

cost_per_base_unit = price_cents / (package_quantity × package_factor)
ingredient_cost    = cost_per_base_unit × (usage_quantity × usage_factor)
total_recipe_cost  = Σ ingredient_cost
cost_per_portion   = total_recipe_cost / servings
suggested_price    = cost_per_portion × 10000 / (10000 - target_margin_bps)
```

### Worked Example — Chocolate Cake (12 servings, 50% margin)

```
Flour:     price_cents=2000, package_quantity=2, package_unit="kg"
           usage_quantity=500, usage_unit="g"
           package_factor=1000, usage_factor=1
           cost_per_g = 2000 / (2 × 1000) = 1 cent/g
           cost = 1 × (500 × 1) = 500 cents

Chocolate: price_cents=1500, package_quantity=200, package_unit="g"
           usage_quantity=300, usage_unit="g"
           cost_per_g = 1500 / 200 = 7.5 cents/g
           cost = 7.5 × 300 = 2250 cents

total_cost       = 500 + 2250 = 2750 cents (R$27.50)
cost_per_portion = 2750 / 12  = 229 cents  (R$2.29)
suggested_price  = 229 × 10000 / (10000 - 5000) = 458 cents (R$4.58)
```

### Edge Cases

| Case | Behaviour |
|---|---|
| Recipe with no ingredients | `total_cost = 0`, `cost_per_portion = 0` |
| Ingredient with no price history | `ingredient_cost = null`, `total_cost = null` |
| Soft-deleted ingredient in existing recipe | still calculates normally |
| `target_margin_bps = 0` | `suggested_price = cost_per_portion` |
| `target_margin_bps = 10000` | rejected with HTTP 422 (division by zero) |

---

## API Contract

### Errors (RFC 7807)

```json
{ "title": "Unprocessable Entity", "status": 422,
  "detail": "Validation failed", "errors": "..." }
```

### Auth Endpoints (no token required)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/auth/login` | Returns Google OAuth redirect URL |
| POST | `/auth/callback` | Exchange OAuth code, upsert user, return JWT pair |
| POST | `/auth/refresh` | Exchange refresh token for new token pair |

### Ingredients

| Method | Endpoint | Status codes |
|---|---|---|
| POST | `/ingredients` | 201, 422 |
| GET | `/ingredients` | 200 |
| GET | `/ingredients/:id` | 200, 404 |
| PATCH | `/ingredients/:id` | 200, 404, 422 |
| DELETE | `/ingredients/:id` | 204, 404 — soft-delete |

**POST /ingredients:**
```json
{ "name": "Farinha", "base_unit": "kg" }
```
Adapter converts `"kg"` → `"g"` before storing.

**Response:**
```json
{ "id": "uuid", "name": "Farinha", "base_unit": "g" }
```

### Price History

| Method | Endpoint | Status codes |
|---|---|---|
| POST | `/ingredients/:id/prices` | 201, 404, 422 |
| GET | `/ingredients/:id/prices` | 200, 404 — newest first |

**POST /ingredients/:id/prices:**
```json
{ "price": 20.00, "package_quantity": 2, "package_unit": "kg", "purchase_date": "2026-03-10" }
```
`package_unit` must be compatible with the ingredient's `base_unit`. Stored as entered.

### Recipes

| Method | Endpoint | Status codes |
|---|---|---|
| POST | `/recipes` | 201, 422 |
| GET | `/recipes` | 200 |
| GET | `/recipes/:id` | 200, 404 |
| PATCH | `/recipes/:id` | 200, 404, 422 |
| DELETE | `/recipes/:id` | 204, 404 — soft-delete |

**POST /recipes:**
```json
{
  "name": "Bolo de Chocolate",
  "servings": 12,
  "target_margin": 50.0,
  "ingredients": [
    { "id": "uuid", "usage_quantity": 500, "usage_unit": "g" },
    { "id": "uuid", "usage_quantity": 300, "usage_unit": "g" }
  ]
}
```
`target_margin` is a percentage — stored internally as bps (`50.0 → 5000`).

---

## Unit Conversion Map (`logic/unit_conversion.clj`)

```clojure
{"kg"    {:base "g"    :factor 1000}
 "g"     {:base "g"    :factor 1}
 "l"     {:base "ml"   :factor 1000}
 "ml"    {:base "ml"   :factor 1}
 "dozen" {:base "unit" :factor 12}
 "unit"  {:base "unit" :factor 1}}
```

Compatible unit groups (enforced in controllers):
- Mass: `g`, `kg`
- Volume: `ml`, `l`
- Count: `unit`, `dozen`

---

## Glossary

| Term | Definition |
|---|---|
| **basis points (bps)** | 1/100 of a percentage point. 5000 bps = 50%. Used for lossless margin storage. |
| **append-only** | Insert-only table. No UPDATEs or DELETEs allowed. Guarantees immutable history. |
| **soft-delete** | Setting `is_active = false` instead of removing the row. |
| **tenant isolation** | Every user sees only their own data, enforced by mandatory `user_id` filter in all queries. |
| **base unit** | Smallest internal unit of measure: `g` (mass), `ml` (volume), `unit` (countable). |
| **wire layer** | The `wire/in`, `wire/out`, and `wire/db` folders. Format schemas for API boundaries. |
| **RFC 7807** | HTTP standard for structured error responses (Problem Details). |
