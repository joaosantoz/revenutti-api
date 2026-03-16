(ns revenu-api.controllers.ingredient
  (:require [revenu-api.adapters.ingredient :as adapter]
            [revenu-api.db.ingredient :as db]))

(defn create-ingredient! [ingredient database]
  (-> (db/insert! database ingredient)
      (adapter/db->internal)))
