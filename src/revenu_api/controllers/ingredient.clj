(ns revenu-api.controllers.ingredient
  (:require [revenu-api.adapters.ingredient :as adapter]
            [revenu-api.db.ingredient :as db]))

(defn create-ingredient!
  [ingredient database]
  (-> (db/insert! database ingredient)
      (adapter/db->internal)))

(defn get-ingredients!
  [database]
  (->> (db/get-all! database)
       (mapv adapter/db->internal)))

(defn find-ingredient!
  [database id]
  (when-let [row (db/find! database id)]
    (adapter/db->internal row)))

(defn update-ingredient!
  [database id ingredient]
  (when-let [row (db/update! database id ingredient)]
    (adapter/db->internal row)))

(defn delete-ingredient!
  [database id]
  (db/deactivate! database id))