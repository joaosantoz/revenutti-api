ALTER TABLE recipe_ingredients ALTER COLUMN usage_quantity TYPE INTEGER USING usage_quantity::INTEGER;
ALTER TABLE price_history ALTER COLUMN package_quantity TYPE INTEGER USING package_quantity::INTEGER;
