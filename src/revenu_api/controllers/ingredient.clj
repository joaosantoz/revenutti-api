(ns revenu-api.controllers.ingredient
  (:require [clojure.pprint :refer [pprint]]
            [revenu-api.adapters.ingredient :as adapter]
            [revenu-api.db.ingredient :as db]
            [schema.core :as s]))

(defn create-ingredient!
  [ingredient database]
  (-> (db/insert! database ingredient)
      (adapter/db->internal)))

(s/defn get-ingredients!
  [database]
  (->> (db/get-all! database)
       (mapv adapter/db->internal)))
