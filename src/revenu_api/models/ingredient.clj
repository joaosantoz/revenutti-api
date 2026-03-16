(ns revenu-api.models.ingredient
  (:require [schema.core :as s]))

(def unit-types #{:g :ml :unit})

(s/defschema UnitType
  (apply s/enum unit-types))

(s/defschema Ingredient
  {:id        s/Uuid
   :name      s/Str
   :unit      UnitType
   :is-active s/Bool})
