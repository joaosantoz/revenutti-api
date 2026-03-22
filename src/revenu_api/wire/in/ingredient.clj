(ns revenu-api.wire.in.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [BaseUnitType]]))

(s/defschema NewIngredient
  {:name                     s/Str
   :base_unit                BaseUnitType
   (s/optional-key :brand)   (s/maybe s/Str)})

(s/defschema UpdateIngredient
  {(s/optional-key :name)    s/Str
   (s/optional-key :brand)   (s/maybe s/Str)})
