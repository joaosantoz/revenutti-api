(ns revenu-api.controllers.ingredient
  (:require [revenu-api.adapters.ingredient :as adapter]
            [revenu-api.db.ingredient :as db]))

(defn create-ingredient!
  [ingredient database user-id]
  (-> (db/insert! database user-id ingredient)
      (adapter/db->internal)))

(defn get-ingredients!
  [database user-id]
  (->> (db/get-all! database user-id)
       (mapv adapter/db->internal)))

(defn find-ingredient!
  [database user-id id]
  (when-let [row (db/find! database user-id id)]
    (adapter/db->internal row)))

(defn update-ingredient!
  [database user-id id ingredient]
  (when-let [row (db/update! database user-id id ingredient)]
    (adapter/db->internal row)))

(defn delete-ingredient!
  [database user-id id]
  (db/deactivate! database user-id id))
