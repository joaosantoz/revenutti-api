(ns revenu-api.adapters.recipe)

(defn wire->internal
  [recipe]
  (select-keys recipe [:name :yield_portions :target_margin_bps]))

(defn db->internal
  [row]
  {:id                (:recipes/id row)
   :name              (:recipes/name row)
   :yield-portions    (:recipes/yield_portions row)
   :target-margin-bps (:recipes/target_margin_bps row)
   :is-active         (:recipes/is_active row)
   :created-at        (:recipes/created_at row)
   :updated-at        (:recipes/updated_at row)})

(defn internal->wire
  [recipe]
  (select-keys recipe [:id :name :yield-portions :target-margin-bps]))

(defn internal+dates->wire
  [recipe]
  (select-keys recipe [:id :name :yield-portions :target-margin-bps :created-at :updated-at]))
