(ns revenu-api.controllers.price-history
  (:require [revenu-api.adapters.price-history :as adapter]
            [revenu-api.db.ingredient :as ingredient.db]
            [revenu-api.db.price-history :as price-history.db]))

(defn add-price!
  [database user-id ingredient-id price-history]
  (when-not (ingredient.db/find! database user-id ingredient-id)
    (throw (ex-info "Ingredient not found" {:status 404})))
  (-> (price-history.db/insert! database ingredient-id price-history)
      (adapter/db->internal)))

(defn get-prices!
  [database user-id ingredient-id]
  (when-not (ingredient.db/find! database user-id ingredient-id)
    (throw (ex-info "Ingredient not found" {:status 404})))
  (->> (price-history.db/get-all! database user-id ingredient-id)
       (mapv adapter/db->internal)))
