(ns revenu-api.wire.db.ingredient
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [BaseUnitType]]))

(s/defschema Ingredient
  {:ingredients/id                       s/Uuid
   :ingredients/name                     s/Str
   :ingredients/base_unit                BaseUnitType
   :ingredients/is_active                s/Bool
   (s/optional-key :ingredients/brand)   (s/maybe s/Str)})
