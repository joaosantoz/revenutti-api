(ns revenu-api.adapters.price-history
  (:require [schema.core :as s]
            [revenu-api.wire.in.price-history :as in]
            [revenu-api.wire.out.price-history :as out]
            [revenu-api.models.price-history :as models]
            [revenu-api.wire.db.price-history :as db]))

(s/defn wire->internal :- models/PriceHistory
  [price-history :- in/NewPriceHistory]
  {:price-cents   (int (* (:price price-history) 100M))
   :package-quantity  (:package_quantity price-history)
   :package-unit  (:package_unit price-history)
   :purchase-date (:purchase_date price-history)})

(s/defn db->internal :- models/PriceHistory
  [row :- db/PriceHistory]
  {:id            (:price_history/id row)
   :ingredient-id (:price_history/ingredient_id row)
   :price-cents   (:price_history/price_cents row)
   :package-quantity  (:price_history/package_quantity row)
   :package-unit  (:price_history/package_unit row)
   :purchase-date (str (:price_history/purchase_date row))})

(s/defn internal->wire :- out/PriceHistory
  [price-history :- models/PriceHistory]
  {:price         (/ (:price-cents price-history) 100M)
   :package_quantity  (:package-quantity price-history)
   :package_unit  (:package-unit price-history)
   :purchase_date (:purchase-date price-history)})
