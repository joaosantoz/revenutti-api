(ns revenu-api.wire.out.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.ingredient :refer [UnitType]]))

(s/defschema Ingredient
  {:id         s/Uuid
   :name       s/Str
   :unit       UnitType})
