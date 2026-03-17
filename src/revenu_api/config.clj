(ns revenu-api.config
  (:require [environ.core :refer [env]]))

(def db-url      (env :db-url))
(def db-user     (env :db-user))
(def db-password (env :db-password))
(def app-env     (env :app-env))
(def port        (Integer/parseInt (env :port)))

(def google-client-id     (env :google-client-id))
(def google-client-secret (env :google-client-secret))
(def google-redirect-uri  (env :google-redirect-uri))
(def jwt-secret           (env :jwt-secret))
