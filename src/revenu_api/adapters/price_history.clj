(ns revenu-api.adapters.price-history
  (:require [schema.core :as s]
            [revenu-api.wire.in.price-history :as in]
            [revenu-api.wire.out.price-history :as out]
            [revenu-api.models.price-history :as models]
            [revenu-api.wire.db.price-history :as db]))

(s/defn wire->internal :- models/NewPriceHistory
  [price-history :- in/NewPriceHistory]
  (cond-> {:price-cents      (int (* (:price price-history) 100M))
           :package-quantity (:package_quantity price-history)
           :package-unit     (:package_unit price-history)
           :purchase-date    (:purchase_date price-history)}
    (contains? price-history :supplier) (assoc :supplier (:supplier price-history))))

(s/defn db->internal :- models/PriceHistory
  [row :- db/PriceHistory]
  (cond-> {:id               (:price_history/id row)
           :ingredient-id    (:price_history/ingredient_id row)
           :price-cents      (:price_history/price_cents row)
           :package-quantity (:price_history/package_quantity row)
           :package-unit     (:price_history/package_unit row)
           :purchase-date    (str (:price_history/purchase_date row))}
    (contains? row :price_history/supplier) (assoc :supplier (:price_history/supplier row))))

(s/defn internal->wire :- out/PriceHistory
  [price-history :- models/PriceHistory]
  (cond-> {:price            (/ (:price-cents price-history) 100M)
           :package_quantity (:package-quantity price-history)
           :package_unit     (:package-unit price-history)
           :purchase_date    (:purchase-date price-history)}
    (:supplier price-history) (assoc :supplier (:supplier price-history))))
