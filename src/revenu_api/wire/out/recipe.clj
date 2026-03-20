(ns revenu-api.wire.out.recipe
  (:require [schema.core :as s]))

(s/defschema RecipeIngredient
  {:id        s/Str
   :usage_qty s/Int
   :name      s/Str
   :unit      s/Str})

(s/defschema Recipe
  {:id                s/Uuid
   :name              s/Str
   :yield_portions    s/Int
   :target_margin_bps s/Int
   :ingredients       [RecipeIngredient]
   :created_at        s/Str
   :updated_at        s/Str})
