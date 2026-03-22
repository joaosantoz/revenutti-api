(ns revenu-api.models.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [BaseUnitType]]))

(s/defschema Ingredient
  {:id        s/Uuid
   :name      s/Str
   :base-unit BaseUnitType
   :is-active s/Bool})
