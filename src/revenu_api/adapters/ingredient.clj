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

(defn update-wire->internal
  [ingredient]
  (select-keys ingredient [:name :unit]))

(defn internal->wire
  [ingredient]
  {:id   (:id ingredient)
   :name (:name ingredient)
   :unit (:unit ingredient)})

(defn internal+dates->wire
  [ingredient]
  {:id         (:id ingredient)
   :name       (:name ingredient)
   :unit       (:unit ingredient)
   :created_at (:created-at ingredient)
   :updated_at (:updated-at ingredient)})
