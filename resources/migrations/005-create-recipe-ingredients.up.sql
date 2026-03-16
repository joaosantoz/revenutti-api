CREATE TABLE recipe_ingredients (
  id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  recipe_id     UUID        NOT NULL REFERENCES recipes(id),
  ingredient_id UUID        NOT NULL REFERENCES ingredients(id),
  usage_qty     INTEGER     NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
