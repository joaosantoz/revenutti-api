# Revenu — Architecture Reference

> Deep-dive companion to `CLAUDE.md`. Covers layer contracts, request lifecycle,
> business rules, and the full API contract.

## Layer Contracts

### `wire/in/`
- Validates incoming requests with Prismatic Schema
- Converts user-friendly units to internal base units: `kg→g`, `l→ml`, `dozen→unit`
- Converts currency: `reais (float) → cents (int)`
- Converts margin: `percent (float) → basis points (int)`
- **Never** accesses the database or calls logic functions

### `controller/`
- Orchestrates the request: calls `db/` to fetch data, passes it to `logic/` for calculation, formats via `wire/out/`
- **Never** contains business logic or domain rules
- One file per domain: `recipe.clj`, `ingredient.clj`, `price_history.clj`, `auth.clj`

### `logic/`
- Pure functions only — no side effects, no DB access, no HTTP
- All inputs and outputs are integers (cents, base units, bps)
- Cost calculation lives here: `logic/recipe.clj`
- Unit conversion map lives here: `logic/unit_conversion.clj`

### `db/`
- Raw SQL via `next.jdbc`
- **Every single query filters by `user_id`** — this is the multi-tenant isolation boundary
- No business logic here — just fetch/insert/update

### `model/`
- Prismatic Schema definitions for domain entities
- Single source of truth for types
- Imported by `wire/in/`, `wire/out/`, and `db/` layers

### `wire/out/`
- Formats data for JSON responses
- Converts back to human-readable: `cents → reais`, `bps → percent`
- **Never** accesses the database

### `middleware/`
- Implemented as Pedestal interceptors
- `auth.clj` — validates JWT, injects `user_id` into request context
- `rate_limit.clj` — enforces per-user request limits by tier
- `error_handler.clj` — catches exceptions, returns RFC 7807 error responses

---

## Request Lifecycle

```
HTTP Request
  ↓
middleware/auth.clj       → validates JWT, injects user_id
middleware/rate_limit.clj → checks limits by tier
  ↓
wire/in/recipe.clj        → validates Prismatic schema, converts units to base
  ↓
controller/recipe.clj     → orchestrates:
  ├── db/recipe.clj       → fetches data (filtered by user_id)
  └── logic/recipe.clj    → calculates costs (pure function)
  ↓
wire/out/recipe.clj       → formats response (cents→reais, bps→%)
  ↓
JSON Response
```

---

## Business Rules

| ID | Rule | Priority |
|---|---|---|
| BR-S01 | Every DB query filters by `user_id` (tenant isolation) | Critical |
| BR-S02 | Rate limit: 100 req/min (free), 500 req/min (paid) | High |
| BR-N01 | Wire/in converts all units to base before DB write. DB stores only g, ml, unit. | Critical |
| BR-N02 | Recipe cost uses the latest price: `ORDER BY purchase_date DESC, id DESC LIMIT 1` | Critical |
| BR-N03 | `PRICE_HISTORY` is append-only. No UPDATE or DELETE. Ever. | Critical |
| BR-N04 | Soft-deleted ingredients remain in existing recipes for cost history. Cannot be added to new recipes. | High |
| BR-N05 | Recipe with zero ingredients returns `cost = 0`, not an error | Medium |
| BR-N06 | Ingredient with no price history returns `cost = null`, not zero | High |

---

## Database Schema

### USERS
| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| external_provider | VARCHAR(50) | `"google"` |
| external_id | VARCHAR(255) | Unique ID from provider |
| name | VARCHAR(255) | From OAuth profile |
| email | VARCHAR(255) NOT NULL | From OAuth profile |
| created_at / updated_at | TIMESTAMP | DEFAULT NOW() |

Unique constraint: `(external_provider, external_id)`

### INGREDIENTS
| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| user_id | INT FK → USERS | Isolation key |
| name | VARCHAR(255) NOT NULL | |
| unit | VARCHAR(50) NOT NULL | `g`, `ml`, or `unit` (base unit) |
| is_active | BOOLEAN DEFAULT TRUE | Soft-delete flag |
| created_at / updated_at | TIMESTAMP | |

Index: `(user_id, is_active)`

### PRICE_HISTORY
| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| ingredient_id | INT FK → INGREDIENTS | |
| price_cents | INT NOT NULL CHECK > 0 | Price in cents |
| package_qty | INT NOT NULL CHECK > 0 | Quantity in base unit |
| purchase_date | DATE NOT NULL | |
| created_at | TIMESTAMP | |

**Append-only. No UPDATE or DELETE endpoints.**  
Index: `(ingredient_id, purchase_date DESC, id DESC)`

### RECIPES
| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| user_id | INT FK → USERS | Isolation key |
| name | VARCHAR(255) NOT NULL | |
| yield_portions | INT NOT NULL CHECK > 0 | Number of portions |
| target_margin_bps | INT NOT NULL DEFAULT 0 | Target margin in bps (5000 = 50%) |
| is_active | BOOLEAN DEFAULT TRUE | Soft-delete flag |
| created_at / updated_at | TIMESTAMP | |

Index: `(user_id, is_active)`

### RECIPE_INGREDIENTS
| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| recipe_id | INT FK → RECIPES | |
| ingredient_id | INT FK → INGREDIENTS | |
| usage_qty | INT NOT NULL CHECK > 0 | Quantity in base unit |
| created_at | TIMESTAMP | |

Index: `(recipe_id)`

---

## Cost Calculation

All values are integers. No floats in logic layer.

```
cost_per_unit      = price_cents / package_qty
ingredient_cost    = cost_per_unit * usage_qty
total_recipe_cost  = Σ ingredient_cost
cost_per_portion   = total_recipe_cost / yield_portions
suggested_price    = cost_per_portion * 10000 / (10000 - target_margin_bps)
```

### Worked Example — Chocolate Cake (12 portions, 50% margin)

```
Flour:     price_cents=2000, package_qty=2000g, usage_qty=500g
           cost = (2000/2000) * 500 = 500 cents

Chocolate: price_cents=1500, package_qty=200g, usage_qty=300g
           cost = (1500/200) * 300 = 2250 cents

total_cost        = 500 + 2250 = 2750 cents (R$27.50)
cost_per_portion  = 2750 / 12  = 229 cents  (R$2.29)
suggested_price   = 229 * 10000 / (10000 - 5000) = 458 cents (R$4.58)
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

### Response Envelope

```json
// Single resource
{ "data": { ... } }

// List
{ "data": [...], "cursor": "abc123", "has_more": true }

// Error (RFC 7807)
{ "type": "validation_error", "title": "Invalid input", "status": 422,
  "detail": "price_cents must be positive", "errors": [...] }
```

### Auth Endpoints (no token required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/callback` | Receive OAuth code, upsert user, return JWT pair |
| POST | `/auth/refresh` | Exchange refresh token for new access token |
| POST | `/auth/logout` | Invalidate refresh token → 204 |

### Ingredients

| Method | Endpoint | Status codes |
|---|---|---|
| POST | `/ingredients` | 201, 409, 422 |
| GET | `/ingredients` | 200 — lists active, `?search=` |
| GET | `/ingredients/:id` | 200, 404 — includes latest price |
| PUT | `/ingredients/:id` | 200, 404, 409, 422 |
| DELETE | `/ingredients/:id` | 204, 404 — soft-delete |

**POST /ingredients request:**
```json
{ "name": "Farinha", "qty_unit": "kg" }
```
`wire/in` derives the base unit (`g`) automatically.

### Price History

| Method | Endpoint | Status codes |
|---|---|---|
| POST | `/ingredients/:id/prices` | 201, 404, 422 |
| GET | `/ingredients/:id/prices` | 200, 404 — newest first, `?from=`, `?to=` |

**POST request:**
```json
{ "price": 20.00, "package_qty": 2, "package_unit": "kg", "purchase_date": "2026-03-10" }
```
`wire/in` converts: `20.00 → 2000 cents`, `2 kg → 2000 g`.

### Recipes

| Method | Endpoint | Status codes |
|---|---|---|
| POST | `/recipes` | 201, 422 |
| GET | `/recipes` | 200 — with calculated cost summary, `?search=` |
| GET | `/recipes/:id` | 200, 404 — full cost breakdown |
| PUT | `/recipes/:id` | 200, 404, 422 — metadata only |
| DELETE | `/recipes/:id` | 204, 404 — soft-delete |
| POST | `/recipes/:id/ingredients` | 200, 404, 409, 422 |
| PUT | `/recipes/:id/ingredients/:iid` | 200, 404, 422 — update qty |
| DELETE | `/recipes/:id/ingredients/:iid` | 200, 404 |

**POST /recipes request:**
```json
{
  "name": "Bolo de Chocolate",
  "yield_portions": 12,
  "target_margin_percent": 50.0,
  "ingredients": [
    { "ingredient_id": 1, "qty": 500, "unit": "g" },
    { "ingredient_id": 3, "qty": 0.3, "unit": "kg" }
  ]
}
```
`wire/in` converts: `50.0% → 5000 bps`, `0.3 kg → 300 g`.

### Dashboard

| Method | Endpoint | Description |
|---|---|---|
| GET | `/dashboard` | Totals, most/least profitable recipe, recent price changes |

---

## Unit Conversion Map

Lives in `logic/unit_conversion.clj`. Used exclusively by `wire/in`.

```clojure
{"kg"    {:base "g"    :factor 1000}
 "g"     {:base "g"    :factor 1}
 "l"     {:base "ml"   :factor 1000}
 "ml"    {:base "ml"   :factor 1}
 "dozen" {:base "unit" :factor 12}
 "unit"  {:base "unit" :factor 1}}
```

---

## Glossary

| Term | Definition |
|---|---|
| **basis points (bps)** | 1/100 of a percentage point. 5000 bps = 50%. Used for lossless margin storage. |
| **append-only** | Insert-only table. No UPDATEs or DELETEs allowed. Guarantees immutable history. |
| **soft-delete** | Setting `is_active = false` instead of removing the row. |
| **tenant isolation** | Every user sees only their own data, enforced by mandatory `user_id` filter in all queries. |
| **base unit** | Smallest internal unit of measure: `g` (mass), `ml` (volume), `unit` (countable). |
| **wire layer** | The `wire/in` and `wire/out` folders. Only place where format conversion happens. |
| **RFC 7807** | HTTP standard for structured error responses (Problem Details). |