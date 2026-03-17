(ns revenu-api.adapters.recipe
  (:require [cheshire.core :as json]))

(defn- parse-ingredients [raw]
  (when raw
    (->> (json/parse-string (.getValue raw) true)
         (remove #(nil? (:id %))))))

(defn wire->internal
  [recipe]
  (select-keys recipe [:name :yield_portions :target_margin_bps :ingredients]))

(defn db->internal
  [row]
  {:id                (:recipes/id row)
   :name              (:recipes/name row)
   :yield-portions    (:recipes/yield_portions row)
   :target-margin-bps (:recipes/target_margin_bps row)
   :is-active         (:recipes/is_active row)
   :created-at        (:recipes/created_at row)
   :updated-at        (:recipes/updated_at row)})

(defn recipe+ingredients->internal
  [row]
  (assoc (db->internal row)
         :ingredients (or (parse-ingredients (:ingredients row)) [])))

(defn internal->wire
  [recipe]
  {:id                (:id recipe)
   :name              (:name recipe)
   :yield_portions    (:yield-portions recipe)
   :target_margin_bps (:target-margin-bps recipe)
   :ingredients       (:ingredients recipe)})

(defn internal+dates->wire
  [recipe]
  {:id                (:id recipe)
   :name              (:name recipe)
   :yield_portions    (:yield-portions recipe)
   :target_margin_bps (:target-margin-bps recipe)
   :ingredients       (:ingredients recipe)
   :created_at        (:created-at recipe)
   :updated_at        (:updated-at recipe)})
