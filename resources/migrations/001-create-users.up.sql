CREATE TABLE users (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  external_provider TEXT        NOT NULL,
  external_id       TEXT        NOT NULL,
  name              TEXT        NOT NULL,
  email             TEXT        NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (external_provider, external_id)
);
