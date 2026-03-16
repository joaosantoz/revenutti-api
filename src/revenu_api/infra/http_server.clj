(ns revenu-api.infra.http-server
  (:require [io.pedestal.http.route :as route]
            [revenu-api.adapters.ingredient :as adapters.ingredient]
            [revenu-api.controllers.ingredient :as controllers.ingredient]
            [schema.core :as s]))

(defn healthcheck
  [_request]
  {:status 200 :body {:status "ok"}})

(defn create-ingredient!
  [{db         :db
    ingredient :json-params}]
  {:status 201
   :body (-> ingredient
             (adapters.ingredient/wire->internal)
             (controllers.ingredient/create-ingredient! db)
             (adapters.ingredient/internal->wire))})

(def api-routes
  #{["/healthcheck"
     :get healthcheck :route-name :healthcheck]
    ["/ingredients"
     :post create-ingredient! :route-name :create-ingredient]})

(def routes
  (route/expand-routes api-routes))