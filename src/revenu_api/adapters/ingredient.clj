(ns revenu-api.adapters.ingredient
  (:require [schema.core :as s]
            [revenu-api.wire.in.ingredient :as in]
            [revenu-api.wire.out.ingredient :as out]
            [revenu-api.models.ingredient :as models]))

(s/defn wire->internal
  [ingredient :- in/NewIngredient]
  ingredient)

(s/defn db->internal
  [row]
  {:id         (:ingredients/id row)
   :name       (:ingredients/name row)
   :unit       (:ingredients/unit row)
   :is-active  (:ingredients/is_active row)
   :created-at (:ingredients/created_at row)
   :updated-at (:ingredients/updated_at row)})

(s/defn update-wire->internal :- models/Ingredient
  [ingredient :- in/UpdateIngredient]
  (select-keys ingredient [:name :unit]))

(s/defn internal->wire :- out/Ingredient
  [{:keys [id name unit created-at updated-at]} :- models/Ingredient]
  (cond-> {:id         id
           :name       name
           :unit       unit
           :created_at created-at}
          updated-at (assoc :updated_at updated-at)))
