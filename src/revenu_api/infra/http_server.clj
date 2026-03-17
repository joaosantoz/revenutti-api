(ns revenu-api.infra.http-server
  (:require [io.pedestal.http.route :as route]
            [revenu-api.adapters.ingredient :as adapters.ingredient]
            [revenu-api.controllers.auth :as controllers.auth]
            [revenu-api.controllers.ingredient :as controllers.ingredient]
            [revenu-api.controllers.price-history :as controllers.price-history]
            [revenu-api.adapters.price-history :as adapters.price-history]
            [revenu-api.controllers.recipe :as controllers.recipe]
            [revenu-api.adapters.recipe :as adapters.recipe]
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
   :body (->> ingredient
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

(defn add-price!
  [{db          :db
    user-id     :user-id
    price       :json-params
   {:keys [id]} :path-params}]
  (try
    (let [result (->> price
                      (adapters.price-history/wire->internal)
                      (controllers.price-history/add-price! db user-id id)
                      (adapters.price-history/internal->wire))]
      {:status 201 :body result})
    (catch Exception e
      {:status (:status (ex-data e) 500)
       :body   {:title  "Error"
                :status (:status (ex-data e) 500)
                :detail (ex-message e)}})))

(defn get-prices!
  [{db          :db
    user-id     :user-id
   {:keys [id]} :path-params}]
  (try
    {:status 200
     :body   (->> (controllers.price-history/get-prices! db user-id id)
                  (mapv adapters.price-history/internal->wire))}
    (catch Exception e
      {:status (:status (ex-data e) 500)
       :body   {:title  "Error"
                :status (:status (ex-data e) 500)
                :detail (ex-message e)}})))

(defn- protected
  [handler]
  [interceptors/jwt-interceptor handler])

(defn create-recipe!
  [{db :db user-id :user-id recipe :json-params}]
  {:status 201
   :body (->> recipe
              (adapters.recipe/wire->internal)
              (controllers.recipe/create-recipe! db user-id)
              (adapters.recipe/internal->wire))})

(defn get-recipes!
  [{db :db user-id :user-id}]
  {:status 200
   :body (->> (controllers.recipe/get-recipes! db user-id)
              (mapv adapters.recipe/internal+dates->wire))})

(defn find-recipe!
  [{db :db user-id :user-id {:keys [id]} :path-params}]
  (if-let [recipe (controllers.recipe/find-recipe! db user-id id)]
    {:status 200 :body (adapters.recipe/internal+dates->wire recipe)}
    {:status 404 :body {:title "Not Found" :detail "Recipe not found" :status 404}}))

(defn update-recipe!
  [{db :db user-id :user-id recipe :json-params {:keys [id]} :path-params}]
  (if-let [updated (->> recipe
                        (adapters.recipe/wire->internal)
                        (controllers.recipe/update-recipe! db user-id id))]
    {:status 200 :body (adapters.recipe/internal+dates->wire updated)}
    {:status 404 :body {:title "Not Found" :detail "Recipe not found" :status 404}}))

(defn delete-recipe!
  [{db :db user-id :user-id {:keys [id]} :path-params}]
  (if-let [deleted? (controllers.recipe/delete-recipe! db user-id id)]
    {:status 204 :body {}}
    {:status 404 :body {:title "Not Found" :detail "Recipe not found" :status 404}}))

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
     :get (protected get-ingredients!)
     :route-name :get-ingredients]

    ["/ingredients"
     :post (protected create-ingredient!)
     :route-name :create-ingredient]

    ["/ingredients/:id"
     :get (protected find-ingredient!)
     :route-name :find-ingredient]

    ["/ingredients/:id"
     :patch (protected update-ingredient!)
     :route-name :update-ingredient]

    ["/ingredients/:id"
     :delete (protected delete-ingredient!)
     :route-name :delete-ingredient]

    ["/ingredients/:id/prices"
     :post (protected add-price!)
     :route-name :add-price]

    ["/ingredients/:id/prices"
     :get (protected get-prices!)
     :route-name :get-prices]

    ["/recipes"
     :post (protected create-recipe!)
     :route-name :create-recipe]

    ["/recipes"
     :get (protected get-recipes!)
     :route-name :get-recipes]

    ["/recipes/:id"
     :get (protected find-recipe!)
     :route-name :find-recipe]

    ["/recipes/:id"
     :patch (protected update-recipe!)
     :route-name :update-recipe]

    ["/recipes/:id"
     :delete (protected delete-recipe!)
     :route-name :delete-recipe]})

(def routes
  (route/expand-routes api-routes))