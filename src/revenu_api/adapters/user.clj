(ns revenu-api.adapters.user
  (:require [schema.core :as s]
            [revenu-api.models.user :refer [User]]))

(s/defn db->internal :- User
  [row]
  {:id                (:users/id row)
   :external-provider (:users/external_provider row)
   :external-id       (:users/external_id row)
   :name              (:users/name row)
   :email             (:users/email row)})
