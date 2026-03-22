(ns revenu-api.adapters.recipe
  (:require [cheshire.core :as json]
            [schema.core :as s]
            [revenu-api.wire.out.recipe :as out.recipe]
            [revenu-api.wire.in.recipe :as in.recipe]
            [revenu-api.models.recipe :as models.recipe]
            [revenu-api.wire.db.recipe :as db.recipe]))

(s/defn ^:private parse-ingredients :- (s/maybe [out.recipe/RecipeIngredient])
  [raw]
  (when raw
    (->> (json/parse-string (.getValue raw) true)
         (remove #(nil? (:id %))))))

(s/defn wire->internal :- models.recipe/Recipe
  [recipe :- in.recipe/Recipe]
  (assoc (select-keys recipe [:name :servings :ingredients])
    :target-margin-bps (int (* (:target_margin recipe) 100M))))


(s/defn db->internal :- models.recipe/Recipe
  [row :- db.recipe/Recipe]
  (assoc {:id                (:recipes/id row)
          :name              (:recipes/name row)
          :servings          (:recipes/servings row)
          :target-margin-bps (:recipes/target_margin_bps row)
          :is-active         (:recipes/is_active row)}
    :ingredients (or (parse-ingredients (:ingredients row)) [])))

(s/defn update-wire->internal :- models.recipe/Recipe
  [recipe :- in.recipe/UpdateRecipe]
  (cond-> (select-keys recipe [:name :servings :ingredients])
    (:target_margin recipe) (assoc :target-margin-bps (* (:target_margin recipe) 100))))

(s/defn internal->wire :- out.recipe/Recipe
  [recipe :- models.recipe/Recipe]
  {:id            (:id recipe)
   :name          (:name recipe)
   :servings      (:servings recipe)
   :target_margin (/ (:target-margin-bps recipe) 100M)
   :ingredients   (:ingredients recipe)})
