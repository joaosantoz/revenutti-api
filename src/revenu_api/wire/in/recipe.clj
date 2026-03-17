(ns revenu-api.wire.in.recipe
  (:require [schema.core :as s]))

(s/defschema NewRecipe
  {:name              s/Str
   :yield_portions    s/Int
   :target_margin_bps s/Int})

(s/defschema UpdateRecipe
  {(s/optional-key :name)              s/Str
   (s/optional-key :yield_portions)    s/Int
   (s/optional-key :target_margin_bps) s/Int})
