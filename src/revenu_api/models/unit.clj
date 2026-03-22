(ns revenu-api.models.unit
  (:require [schema.core :as s]))

(s/defschema BaseUnitType (s/enum "g" "ml" "unit"))
(s/defschema WireUnitType (s/enum "kg" "g" "l" "ml" "dozen" "unit"))
