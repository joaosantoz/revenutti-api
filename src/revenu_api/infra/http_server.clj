(ns revenu-api.infra.http-server
  (:require [io.pedestal.http.route :as route]
            [revenu-api.adapters.ingredient :as adapters.ingredient]
            [revenu-api.controllers.auth :as controllers.auth]
            [revenu-api.controllers.ingredient :as controllers.ingredient]
            [revenu-api.interceptors :as interceptors]))

(defn healthcheck
  [_request]
  {:status 200
   :body {:status :ok}})

(defn auth-callback!
  [{db            :db
   {:keys [code]} :json-params}]
  (let [decoded-code (java.net.URLDecoder/decode code "UTF-8")
        tokens       (controllers.auth/google-callback! db decoded-code)]
    {:status 200
     :body   {:access_token  (:access-token tokens)
              :refresh_token (:refresh-token tokens)
              :token_type    "Bearer"
              :expires_in    900}}))

(defn auth-refresh!
  [{db                     :db
   {:keys [refresh-token]} :json-params}]
  (try
    (let [tokens (controllers.auth/refresh-tokens! db refresh-token)]
      {:status 200
       :body   {:access_token  (:access-token tokens)
                :refresh_token (:refresh-token tokens)
                :token_type    "Bearer"
                :expires_in    900}})
    (catch Exception e
      {:status 401
       :body   {:title  "Unauthorized"
                :status 401
                :detail (ex-message e)}})))

(defn create-ingredient!
  [{db         :db
    user-id    :user-id
    ingredient :json-params}]
  {:status 201
   :body (-> ingredient
             (adapters.ingredient/wire->internal)
             (controllers.ingredient/create-ingredient! db user-id)
             (adapters.ingredient/internal->wire))})

(defn get-ingredients!
  [{db :db user-id :user-id}]
  {:status 200
   :body (->> (controllers.ingredient/get-ingredients! db user-id)
              (mapv adapters.ingredient/internal+dates->wire))})

(defn find-ingredient!
  [{db          :db
    user-id     :user-id
   {:keys [id]} :path-params}]
  (if-let [ingredient (controllers.ingredient/find-ingredient! db user-id id)]
    {:status 200
     :body   (adapters.ingredient/internal+dates->wire ingredient)}
    {:status 404
     :body   {:title  "Not Found"
              :detail "Ingredient not found"
              :status 404}}))

(defn update-ingredient!
  [{db           :db
    user-id      :user-id
    ingredient   :json-params
   {:keys [id]}  :path-params}]
  (if-let [updated (->> ingredient
                        (adapters.ingredient/update-wire->internal)
                        (controllers.ingredient/update-ingredient! db user-id id))]
    {:status 200
     :body   (adapters.ingredient/internal+dates->wire updated)}
    {:status 404
     :body   {:title  "Not Found"
              :detail "Ingredient not found"
              :status 404}}))

(defn delete-ingredient!
  [{db          :db
    user-id     :user-id
   {:keys [id]} :path-params}]
  (if-let [deleted? (controllers.ingredient/delete-ingredient! db user-id id)]
    {:status 204
     :body   {}}
    {:status 404
     :body   {:title  "Not Found"
              :detail "Ingredient not found"
              :status 404}}))

(def api-routes
  #{["/healthcheck"
     :get healthcheck
     :route-name :healthcheck]

    ["/auth/callback"
     :post auth-callback!
     :route-name :auth-callback]

    ["/auth/refresh"
     :post auth-refresh!
     :route-name :auth-refresh]

    ["/ingredients"
     :get [interceptors/jwt-interceptor get-ingredients!]
     :route-name :get-ingredients]

    ["/ingredients"
     :post [interceptors/jwt-interceptor create-ingredient!]
     :route-name :create-ingredient]

    ["/ingredients/:id"
     :get [interceptors/jwt-interceptor find-ingredient!]
     :route-name :find-ingredient]

    ["/ingredients/:id"
     :patch [interceptors/jwt-interceptor update-ingredient!]
     :route-name :update-ingredient]

    ["/ingredients/:id"
     :delete [interceptors/jwt-interceptor delete-ingredient!]
     :route-name :delete-ingredient]})

(def routes
  (route/expand-routes api-routes))