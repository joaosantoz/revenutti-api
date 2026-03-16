(ns revenu-api.wire.in.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.ingredient :refer [UnitType]]))

(s/defschema NewIngredient
  {:name s/Str
   :unit UnitType})

(s/defschema UpdateIngredient
  {(s/optional-key :name) s/Str
   (s/optional-key :unit) UnitType})
