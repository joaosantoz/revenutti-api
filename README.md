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

## License

Copyright © 2026 João Santos

Licensed under the [Apache License, Version 2.0](LICENSE).
