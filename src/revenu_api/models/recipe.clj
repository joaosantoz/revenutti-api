(ns revenu-api.models.recipe
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [WireUnitType]]))

(s/defschema Recipe
  {:id                s/Uuid
   :name              s/Str
   :servings          s/Int
   :target-margin-bps s/Int
   :is-active         s/Bool})

(s/defschema CostIngredient
  {:id                       s/Str
   :name                     s/Str
   (s/optional-key :brand)   (s/maybe s/Str)
   :usage-quantity           s/Num
   :usage-unit               WireUnitType
   :price-cents              (s/maybe s/Int)
   :package-quantity         (s/maybe s/Num)
   :package-unit             (s/maybe WireUnitType)
   (s/optional-key :cost)    (s/maybe s/Num)})

(s/defschema RecipePricing
  {:total-cost                  (s/maybe s/Num)
   :portion-cost            (s/maybe s/Num)
   :suggested-portion-price (s/maybe s/Num)
   :suggested-total-price       (s/maybe s/Num)
   :ingredients                 [CostIngredient]})
