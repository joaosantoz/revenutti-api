(ns revenu-api.controllers.recipe
  (:require [revenu-api.adapters.recipe :as adapter]
            [revenu-api.db.recipe :as db]))

(defn find-recipe!
  [database user-id id]
  (when-let [row (db/find! database user-id id)]
    (adapter/db->internal row)))

(defn create-recipe!
  [database user-id recipe]
  (let [row (db/insert! database user-id recipe)]
    (find-recipe! database user-id (str (:recipes/id row)))))

(defn get-recipes!
  [database user-id]
  (->> (db/get-all! database user-id)
       (mapv adapter/db->internal)))

(defn update-recipe!
  [database user-id id recipe]
  (when (db/update! database user-id id recipe)
    (find-recipe! database user-id id)))

(defn delete-recipe!
  [database user-id id]
  (db/deactivate! database user-id id))
