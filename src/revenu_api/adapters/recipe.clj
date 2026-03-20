(ns revenu-api.adapters.recipe
  (:require [cheshire.core :as json]
            [schema.core :as s]
            [revenu-api.wire.out.recipe :as out]
            [revenu-api.models.ingredient :as models.ingredient]
            [revenu-api.models.recipe :as models.recipe]))

(s/defn ^:private parse-ingredients :- (s/maybe [models.ingredient/Ingredient])
  [raw]
  (when raw
    (->> (json/parse-string (.getValue raw) true)
         (remove #(nil? (:id %))))))

(s/defn wire->internal :- models.recipe/Recipe
  [recipe :- out/Recipe]
  (select-keys recipe [:name :yield_portions :target_margin_bps :ingredients]))

(s/defn db->internal :- models.recipe/Recipe
  [row]
  (assoc {:id                (:recipes/id row)
          :name              (:recipes/name row)
          :yield-portions    (:recipes/yield_portions row)
          :target-margin-bps (:recipes/target_margin_bps row)
          :is-active         (:recipes/is_active row)
          :created-at        (:recipes/created_at row)
          :updated-at        (:recipes/updated_at row)}
          :ingredients       (or (parse-ingredients (:ingredients row)) [])))

(s/defn internal->wire :- out/Recipe
  [recipe :- models.recipe/Recipe]
  {:id                (:id recipe)
   :name              (:name recipe)
   :yield_portions    (:yield-portions recipe)
   :target_margin_bps (:target-margin-bps recipe)
   :ingredients       (:ingredients recipe)
   :created_at        (:created-at recipe)
   :updated_at        (:updated-at recipe)})
