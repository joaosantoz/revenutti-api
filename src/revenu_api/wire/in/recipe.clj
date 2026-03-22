(ns revenu-api.wire.in.recipe
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [WireUnitType]]))

(s/defschema RecipeIngredient
  {:id             s/Str
   :usage_quantity s/Num
   :usage_unit     WireUnitType})

(s/defschema Recipe
  {:name          s/Str
   :servings      s/Int
   :target_margin s/Num
   :ingredients   [RecipeIngredient]})

(s/defschema UpdateRecipe
  {(s/optional-key :name)          s/Str
   (s/optional-key :servings)      s/Int
   (s/optional-key :target_margin) s/Num
   (s/optional-key :ingredients)   [RecipeIngredient]})
