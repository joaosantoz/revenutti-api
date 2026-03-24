(ns revenu-api.controllers.ingredient
  (:require [revenu-api.adapters.ingredient :as adapter]
            [revenu-api.db.ingredient :as ingredient.db]
            [revenu-api.db.price-history :as price-history.db]))

(defn create-ingredient!
  [database user-id ingredient]
  (when (and (:unit-size ingredient) (= (:base-unit ingredient) "unit"))
    (throw (ex-info "unit_size is only valid for ml or g base_unit" {:status 422})))
  (-> (ingredient.db/insert! database user-id ingredient)
      (adapter/db->internal)))

(defn get-ingredients!
  [database user-id]
  (->> (ingredient.db/get-all! database user-id)
       (mapv adapter/db->internal)))

(defn find-ingredient!
  [database user-id id]
  (when-let [row (ingredient.db/find! database user-id id)]
    (adapter/db->internal row)))

(defn update-ingredient!
  [database user-id id ingredient]
  (when (contains? ingredient :unit-size)
    (when (price-history.db/has-unit-purchases? database id)
      (throw (ex-info "unit_size cannot be changed after unit purchases exist" {:status 422}))))
  (when-let [row (ingredient.db/update! database user-id id ingredient)]
    (adapter/db->internal row)))

(defn delete-ingredient!
  [database user-id id]
  (ingredient.db/deactivate! database user-id id))
