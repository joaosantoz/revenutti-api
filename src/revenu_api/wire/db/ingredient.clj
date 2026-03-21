(ns revenu-api.wire.db.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.ingredient :refer [UnitType]]))

(s/defschema Ingredient
  {:ingredients/id        s/Uuid
   :ingredients/name      s/Str
   :ingredients/unit      UnitType
   :ingredients/is_active s/Bool})
