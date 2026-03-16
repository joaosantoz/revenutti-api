(ns revenu-api.wire.out.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.ingredient :refer [UnitType]]))

(s/defschema Ingredient
  {:id         s/Str
   :name       s/Str
   :unit       s/Str
   :is-active  s/Bool
   :created-at s/Str
   :updated-at s/Str})
