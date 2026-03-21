(ns revenu-api.adapters.ingredient
  (:require [schema.core :as s]
            [revenu-api.wire.in.ingredient :as in]
            [revenu-api.wire.out.ingredient :as out]
            [revenu-api.models.ingredient :as models]
            [revenu-api.wire.db.ingredient :as db]))

(s/defn wire->internal :- models/Ingredient
  [ingredient :- in/NewIngredient]
  ingredient)

(s/defn db->internal :- models/Ingredient
  [row :- db/Ingredient]
  {:id         (:ingredients/id row)
   :name       (:ingredients/name row)
   :unit       (:ingredients/unit row)
   :is-active  (:ingredients/is_active row)})

(s/defn update-wire->internal :- models/Ingredient
  [ingredient :- in/UpdateIngredient]
  (select-keys ingredient [:name :unit]))

(s/defn internal->wire :- out/Ingredient
  [{:keys [id name unit]} :- models/Ingredient]
  {:id   id
   :name name
   :unit unit})
