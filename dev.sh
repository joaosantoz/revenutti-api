#!/usr/bin/env bash
set -e

echo "Starting Postgres..."
docker compose -f docker-compose.dev.yml up -d

echo "Waiting for Postgres to be healthy..."
until docker compose -f docker-compose.dev.yml exec db pg_isready -U revenu -d revenu_dev -q; do
  sleep 1
done

echo "Starting REPL..."
lein repl
