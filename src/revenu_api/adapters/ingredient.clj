(ns revenu-api.adapters.ingredient)

(defn wire->internal
  [ingredient]
  (-> ingredient
      (update :unit keyword)))

(defn db->internal
  [row]
  {:id         (:ingredients/id row)
   :name       (:ingredients/name row)
   :unit       (keyword (:ingredients/unit row))
   :is-active  (:ingredients/is_active row)
   :created-at (:ingredients/created_at row)
   :updated-at (:ingredients/updated_at row)})

(defn internal->wire
  [ingredient]
  (select-keys ingredient [:id :name :unit]))

(defn internal+dates->wire
  [ingredient]
  (select-keys ingredient [:id :name :unit :created-at :updated-at]))
