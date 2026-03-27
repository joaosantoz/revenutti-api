(ns revenu-api.wire.db.user
  (:require [schema.core :as s]))

(s/defschema User
  {:users/id                s/Uuid
   :users/external_provider s/Str
   :users/external_id       s/Str
   :users/name                       s/Str
   :users/email                      s/Str
   (s/optional-key :users/avatar_url) (s/maybe s/Str)})
