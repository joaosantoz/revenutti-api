CREATE TABLE recipe_ingredients (
  id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  recipe_id     UUID        NOT NULL REFERENCES recipes(id),
  ingredient_id UUID        NOT NULL REFERENCES ingredients(id),
  usage_quantity INTEGER     NOT NULL,
  usage_unit     TEXT        NOT NULL CHECK (usage_unit IN ('kg', 'g', 'l', 'ml', 'dozen', 'unit')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
