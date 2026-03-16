(ns revenu-api.config
  (:require [environ.core :refer [env]]))

(def db-url      (env :db-url))
(def db-user     (env :db-user))
(def db-password (env :db-password))
(def app-env     (env :app-env))
(def port        (Integer/parseInt (env :port)))
