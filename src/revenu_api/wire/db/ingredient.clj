(ns revenu-api.wire.db.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.ingredient :refer [UnitType]]))

(s/defschema IngredientRow
  {:id         s/Uuid
   :user-id    s/Uuid
   :name       s/Str
   :unit       UnitType
   :is-active  s/Bool
   :created-at s/Inst
   :updated-at s/Inst})
