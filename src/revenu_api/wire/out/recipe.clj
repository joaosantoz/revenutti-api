(ns revenu-api.wire.out.recipe
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [WireUnitType]]))

(s/defschema RecipeIngredient
  {:id                     s/Str
   :usage_quantity         s/Num
   :usage_unit             WireUnitType
   :name                   s/Str
   (s/optional-key :brand) (s/maybe s/Str)})

(s/defschema Recipe
  {:id            s/Uuid
   :name          s/Str
   :servings      s/Int
   :target_margin s/Num
   :ingredients   [RecipeIngredient]})

(s/defschema CostIngredient
  {:id                     s/Str
   :name                   s/Str
   (s/optional-key :brand) (s/maybe s/Str)
   :usage_quantity         s/Num
   :usage_unit             WireUnitType
   :cost                   (s/maybe s/Num)})

(s/defschema RecipePricing
  {:target_margin                    s/Num
   :total_cost                       (s/maybe s/Num)
   :portion_cost                 (s/maybe s/Num)
   :suggested_portion_price      (s/maybe s/Num)
   :suggested_total_price            (s/maybe s/Num)
   (s/optional-key :actual_margin)   (s/maybe s/Num)
   :ingredients                      [CostIngredient]})
