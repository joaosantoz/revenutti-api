CREATE TABLE price_history (
  id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  ingredient_id UUID        NOT NULL REFERENCES ingredients(id),
  price_cents   INTEGER     NOT NULL,
  package_size   INTEGER     NOT NULL,
  purchase_date DATE        NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
