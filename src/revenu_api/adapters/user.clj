(ns revenu-api.adapters.user
  (:require [schema.core :as s]
            [revenu-api.models.user :as models]
            [revenu-api.wire.db.user :as db]))

(s/defn db->internal :- models/User
  [row :- db/User]
  (cond-> {:id                (:users/id row)
           :external-provider (:users/external_provider row)
           :external-id       (:users/external_id row)
           :name              (:users/name row)
           :email             (:users/email row)}
    (contains? row :users/avatar_url) (assoc :avatar-url (:users/avatar_url row))))
