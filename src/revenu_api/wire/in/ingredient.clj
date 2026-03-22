(ns revenu-api.wire.in.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [BaseUnitType]]))

(s/defschema NewIngredient
  {:name      s/Str
   :base_unit BaseUnitType})

(s/defschema UpdateIngredient
  {(s/optional-key :name) s/Str})
