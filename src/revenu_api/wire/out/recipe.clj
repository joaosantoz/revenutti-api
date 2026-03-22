(ns revenu-api.wire.out.recipe
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [WireUnitType]]))

(s/defschema RecipeIngredient
  {:id                     s/Str
   :usage_quantity         s/Int
   :usage_unit             WireUnitType
   :name                   s/Str
   :base_unit              s/Str
   (s/optional-key :brand) (s/maybe s/Str)})

(s/defschema Recipe
  {:id            s/Uuid
   :name          s/Str
   :servings      s/Int
   :target_margin s/Num
   :ingredients   [RecipeIngredient]})
