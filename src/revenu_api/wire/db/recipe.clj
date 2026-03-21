(ns revenu-api.wire.db.recipe
  (:require [schema.core :as s]))

(s/defschema Recipe
  {:recipes/id                s/Uuid
   :recipes/name              s/Str
   :recipes/servings          s/Int
   :recipes/target_margin_bps s/Int
   :recipes/is_active         s/Bool
   :ingredients               s/Any})
