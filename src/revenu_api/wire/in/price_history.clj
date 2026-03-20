(ns revenu-api.wire.in.price-history
  (:require [schema.core :as s]))

(s/defschema NewPriceHistory
  {:price         s/Num
   :package_size  s/Int
   :purchase_date s/Str})
