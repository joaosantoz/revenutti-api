(ns revenu-api.infra.http-server
  (:require [io.pedestal.http.route :as route]))

(defn healthcheck
  [_request]
  {:status 200 :body {:status "ok"}})

(def api-routes
  #{["/healthcheck"
    :get healthcheck :route-name :healthcheck]})

(def routes
  (route/expand-routes api-routes))