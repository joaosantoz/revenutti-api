(ns revenu-api.controllers.recipe
  (:require [revenu-api.adapters.recipe :as adapter]
            [revenu-api.db.recipe :as db]))

(defn create-recipe!
  [database user-id recipe]
  (-> (db/insert! database user-id recipe)
      (adapter/db->internal)
      (assoc :ingredients (:ingredients recipe))))

(defn get-recipes!
  [database user-id]
  (->> (db/get-all! database user-id)
       (mapv adapter/recipe+ingredients->internal)))

(defn find-recipe!
  [database user-id id]
  (when-let [row (db/find! database user-id id)]
    (adapter/recipe+ingredients->internal row)))

(defn update-recipe!
  [database user-id id recipe]
  (when (db/update! database user-id id recipe)
    (find-recipe! database user-id id)))

(defn delete-recipe!
  [database user-id id]
  (db/deactivate! database user-id id))
