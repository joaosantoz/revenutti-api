(ns revenu-api.wire.db.price-history
  (:require [schema.core :as s]))

(s/defschema PriceHistory
  {:price_history/id            s/Uuid
   :price_history/ingredient_id s/Uuid
   :price_history/price_cents   s/Int
   :price_history/package_size  s/Int
   :price_history/purchase_date s/Inst})
