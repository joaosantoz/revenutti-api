(ns revenu-api.wire.out.price-history
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [WireUnitType]]))

(s/defschema PriceHistory
  {:price         s/Num
   :package_quantity  s/Int
   :package_unit  WireUnitType
   :purchase_date s/Str})
