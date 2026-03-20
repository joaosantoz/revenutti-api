(ns revenu-api.models.recipe
  (:require [schema.core :as s]))

(s/defschema Recipe
  {:id                s/Uuid
   :name              s/Str
   :servings    s/Int
   :target-margin-bps s/Int
   :is-active         s/Bool})
