(ns revenu-api.adapters.ingredient
  (:require [schema.core :as s]
            [revenu-api.wire.in.ingredient :as in]
            [revenu-api.wire.out.ingredient :as out]
            [revenu-api.models.ingredient :as models]
            [revenu-api.wire.db.ingredient :as db]))

(s/defn wire->internal :- models/Ingredient
  [ingredient :- in/NewIngredient]
  (assoc (select-keys ingredient [:name :brand])
    :base-unit (:base_unit ingredient)))

(s/defn db->internal :- models/Ingredient
  [row :- db/Ingredient]
  {:id        (:ingredients/id row)
   :name      (:ingredients/name row)
   :base-unit (:ingredients/base_unit row)
   :is-active (:ingredients/is_active row)
   :brand     (:ingredients/brand row)})

(s/defn update-wire->internal :- models/Ingredient
  [ingredient :- in/UpdateIngredient]
  (select-keys ingredient [:name :brand]))

(s/defn internal->wire :- out/Ingredient
  [{:keys [id name base-unit brand]} :- models/Ingredient]
  (cond-> {:id        id
           :name      name
           :base_unit base-unit}
    brand (assoc :brand brand)))
