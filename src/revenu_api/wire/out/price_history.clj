(ns revenu-api.wire.out.price-history
  (:require [schema.core :as s]))

(s/defschema PriceHistory
  {:price         s/Num
   :package_qty   s/Int
   :purchase_date s/Str})
