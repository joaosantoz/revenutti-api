(ns revenu-api.adapters.user
  (:require [schema.core :as s]
            [revenu-api.models.user :as models]
            [revenu-api.wire.db.ingredient :as db]))

(s/defn db->internal :- models/User
  [row :- db/Ingredient]
  {:id                (:users/id row)
   :external-provider (:users/external_provider row)
   :external-id       (:users/external_id row)
   :name              (:users/name row)
   :email             (:users/email row)})
