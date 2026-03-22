(ns revenu-api.controllers.price-history
  (:require [clojure.string :as str]
            [revenu-api.adapters.price-history :as adapter]
            [revenu-api.db.ingredient :as ingredient.db]
            [revenu-api.db.price-history :as price-history.db]
            [revenu-api.logic.unit-conversion :as logic]))

(defn add-price!
  [database user-id ingredient-id price-history]
  (let [ingredient (ingredient.db/find! database user-id ingredient-id)]
    (when-not ingredient
      (throw (ex-info "Ingredient not found" {:status 404})))
    (let [base-unit (:ingredients/base_unit ingredient)]
      (when-not (logic/compatible? base-unit (:package-unit price-history))
        (throw (ex-info (format "Invalid package_unit '%s', accepted: %s"
                                (:package-unit price-history)
                                (str/join ", " (logic/accepted-units base-unit)))
                        {:status 422}))))
    (-> (price-history.db/insert! database ingredient-id price-history)
        (adapter/db->internal))))

(defn get-prices!
  [database user-id ingredient-id]
  (when-not (ingredient.db/find! database user-id ingredient-id)
    (throw (ex-info "Ingredient not found" {:status 404})))
  (->> (price-history.db/get-all! database user-id ingredient-id)
       (mapv adapter/db->internal)))
