(ns revenu-api.wire.out.recipe
  (:require [schema.core :as s]))

(s/defschema RecipeIngredient
  {:id        s/Str
   :quantity  s/Int
   :name      s/Str
   :unit      s/Str})

(s/defschema Recipe
  {:id            s/Uuid
   :name          s/Str
   :servings      s/Int
   :target_margin s/Num
   :ingredients   [RecipeIngredient]})
