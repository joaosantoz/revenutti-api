(ns revenu-api.adapters.ingredient
  (:require [schema.core :as s]
            [revenu-api.wire.in.ingredient :as in]
            [revenu-api.wire.out.ingredient :as out]
            [revenu-api.models.ingredient :as models]
            [revenu-api.wire.db.ingredient :as db]))

(s/defn wire->internal :- models/Ingredient
  [ingredient :- in/NewIngredient]
  (cond-> (assoc (select-keys ingredient [:name :brand])
            :base-unit (:base_unit ingredient))
    (:unit_size ingredient) (assoc :unit-size (:unit_size ingredient))))

(s/defn db->internal :- models/Ingredient
  [row :- db/Ingredient]
  {:id        (:ingredients/id row)
   :name      (:ingredients/name row)
   :base-unit (:ingredients/base_unit row)
   :is-active (:ingredients/is_active row)
   :brand     (:ingredients/brand row)
   :unit-size (:ingredients/unit_size row)})

(s/defn update-wire->internal :- models/Ingredient
  [ingredient :- in/UpdateIngredient]
  (cond-> (select-keys ingredient [:name :brand])
    (contains? ingredient :unit_size) (assoc :unit-size (:unit_size ingredient))))

(s/defn internal->wire :- out/Ingredient
  [{:keys [id name base-unit brand unit-size]} :- models/Ingredient]
  (cond-> {:id        id
           :name      name
           :base_unit base-unit}
    brand     (assoc :brand brand)
    unit-size (assoc :unit_size unit-size)))
