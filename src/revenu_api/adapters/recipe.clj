(ns revenu-api.adapters.recipe
  (:require [cheshire.core :as json]
            [schema.core :as s]
            [revenu-api.wire.out.recipe :as out.recipe]
            [revenu-api.models.recipe :as models.recipe]
            [revenu-api.wire.db.recipe :as db.recipe]))

(s/defn ^:private parse-ingredients :- (s/maybe [out.recipe/RecipeIngredient])
  [raw]
  (when raw
    (->> (json/parse-string (.getValue raw) true)
         (remove #(nil? (:id %))))))

(s/defn wire->internal :- models.recipe/Recipe
  [recipe :- out.recipe/Recipe]
  (select-keys recipe [:name :servings :target_margin_bps :ingredients]))

(s/defn db->internal :- models.recipe/Recipe
  [row :- db.recipe/Recipe]
  (assoc {:id                (:recipes/id row)
          :name              (:recipes/name row)
          :servings          (:recipes/servings row)
          :target-margin-bps (:recipes/target_margin_bps row)
          :is-active         (:recipes/is_active row)}
          :ingredients       (or (parse-ingredients (:ingredients row)) [])))

(s/defn internal->wire :- out.recipe/Recipe
  [recipe :- models.recipe/Recipe]
  {:id                (:id recipe)
   :name              (:name recipe)
   :servings          (:servings recipe)
   :target_margin_bps (:target-margin-bps recipe)
   :ingredients       (:ingredients recipe)})
