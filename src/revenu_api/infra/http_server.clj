(ns revenu-api.infra.http-server
  (:require [io.pedestal.http.route :as route]
            [revenu-api.adapters.ingredient :as adapters.ingredient]
            [revenu-api.controllers.ingredient :as controllers.ingredient]))

(defn healthcheck
  [_request]
  {:status 200 :body {:status :ok}})

(defn create-ingredient!
  [{db         :db
    ingredient :json-params}]
  {:status 201
   :body (-> ingredient
             (adapters.ingredient/wire->internal)
             (controllers.ingredient/create-ingredient! db)
             (adapters.ingredient/internal->wire))})

(defn get-ingredients!
  [{db :db}]
  {:status 200
    :body (->> (controllers.ingredient/get-ingredients! db)
               (mapv adapters.ingredient/internal+dates->wire))})

(defn find-ingredient!
  [{db          :db
   {:keys [id]} :path-params}]
  (if-let [ingredient (controllers.ingredient/find-ingredient! db id)]
    {:status 200
     :body   (adapters.ingredient/internal+dates->wire ingredient)}
    {:status 404
     :body   {:title  "Not Found"
              :detail "Ingredient not found"
              :status 404}}))

(defn update-ingredient!
  [{db          :db
    ingredient  :json-params
   {:keys [id]} :path-params}]
  (if-let [updated (->> ingredient
                       (adapters.ingredient/update-wire->internal)
                       (controllers.ingredient/update-ingredient! db id))]
    {:status 200 :body (adapters.ingredient/internal+dates->wire updated)}
    {:status 404 :body {:title "Not Found" :detail "Ingredient not found" :status 404}}))

(def api-routes
  #{["/healthcheck"
     :get healthcheck
     :route-name :healthcheck]

    ["/ingredients"
     :get get-ingredients!
     :route-name :get-ingredients]

    ["/ingredients"
     :post
     create-ingredient!
     :route-name :create-ingredient]

    ["/ingredients/:id"
     :get find-ingredient!
     :route-name :find-ingredient]

    ["/ingredients/:id"
     :patch update-ingredient!
     :route-name :update-ingredient]})

(def routes
  (route/expand-routes api-routes))