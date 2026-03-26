(ns revenu-api.models.price-history
  (:require [schema.core :as s]
            [revenu-api.models.unit :refer [WireUnitType]]))

(s/defschema PriceHistory
  {:id                        s/Uuid
   :ingredient-id             s/Uuid
   :price-cents               s/Int
   :package-quantity          s/Num
   :package-unit              WireUnitType
   :purchase-date             s/Str
   (s/optional-key :supplier) (s/maybe s/Str)})

(s/defschema NewPriceHistory
  {:price-cents               s/Int
   :package-quantity          s/Num
   :package-unit              WireUnitType
   :purchase-date             s/Str
   (s/optional-key :supplier) (s/maybe s/Str)})
