(ns revenu-api.adapters.price-history)

(defn wire->internal
  [price-history]
  (-> price-history
      (assoc :price-cents (int (* (:price price-history) 100M)))
      (dissoc :price)))

(defn db->internal
  [row]
  {:id            (:price_history/id row)
   :ingredient-id (:price_history/ingredient_id row)
   :price-cents   (:price_history/price_cents row)
   :package-qty   (:price_history/package_qty row)
   :purchase-date (str (:price_history/purchase_date row))})

(defn internal->wire
  [price-history]
  {:price         (/ (:price-cents price-history) 100.0)
   :package_qty   (:package-qty price-history)
   :purchase_date (:purchase-date price-history)})
