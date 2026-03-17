(ns revenu-api.adapters.user)

(defn db->internal
  [row]
  {:id                (:users/id row)
   :external-provider (:users/external_provider row)
   :external-id       (:users/external_id row)
   :name              (:users/name row)
   :email             (:users/email row)})
