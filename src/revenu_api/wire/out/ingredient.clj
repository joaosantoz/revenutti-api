(ns revenu-api.wire.out.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [BaseUnitType]]))

(s/defschema Ingredient
  {:id                         s/Uuid
   :name                       s/Str
   :base_unit                  BaseUnitType
   (s/optional-key :brand)     (s/maybe s/Str)
   (s/optional-key :unit_size) (s/maybe s/Int)})
