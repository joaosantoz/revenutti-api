(ns revenu-api.wire.in.price-history
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [WireUnitType]]))

(s/defschema NewPriceHistory
  {:price                        s/Num
   :package_quantity             s/Num
   :package_unit                 WireUnitType
   :purchase_date                s/Str
   (s/optional-key :supplier)   (s/maybe s/Str)})
