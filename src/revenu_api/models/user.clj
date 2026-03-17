(ns revenu-api.models.user
  (:require [schema.core :as s]))

(s/defschema User
  {:id                s/Uuid
   :external-provider s/Str
   :external-id       s/Str
   :name              s/Str
   :email             s/Str})
