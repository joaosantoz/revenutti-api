(ns revenu-api.interceptors
  (:require [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [revenu-api.logic.auth :as auth]
            [schema.core :as s])
  (:import [java.util UUID]))

(defn db-interceptor
  [db]
  (interceptor/interceptor
    {:name  ::db
     :enter (fn [ctx]
              (assoc-in ctx [:request :db] db))}))

(defn coerce
  [schema]
  (interceptor/interceptor
    {:name  ::coerce
     :enter (fn [ctx]
              (let [params (get-in ctx [:request :json-params])
                    errors (s/check schema params)]
                (if errors
                  (chain/terminate
                    (assoc ctx :response {:status 422
                                          :body   {:title  "Unprocessable Entity"
                                                   :status 422
                                                   :detail "Validation failed"
                                                   :errors errors}}))
                  ctx)))}))

(def error-interceptor
  (interceptor/interceptor
    {:name  ::error
     :error (fn [ctx ex]
              (let [cause  (or (ex-cause ex) ex)
                    data   (ex-data cause)
                    status (get data :status 500)
                    detail (ex-message cause)]
                (assoc ctx :response {:status status
                                      :body   {:title  (if (= 500 status) "Internal Server Error" "Error")
                                               :status status
                                               :detail detail}})))}))

(def jwt-interceptor
  (interceptor/interceptor
    {:name  ::jwt
     :enter (fn [ctx]
              (let [token (some-> (get-in ctx [:request :headers "authorization"])
                                  (clojure.string/replace #"(?i)^bearer " ""))]
                (if-not token
                  (chain/terminate
                    (assoc ctx :response {:status 401
                                          :body   {:title  "Unauthorized"
                                                   :status 401
                                                   :detail "Missing Authorization header"}}))
                  (try
                    (let [claims (auth/verify-token token)]
                      (assoc-in ctx [:request :user-id] (UUID/fromString (:sub claims))))
                    (catch Exception _
                      (chain/terminate
                        (assoc ctx :response {:status 401
                                              :body   {:title  "Unauthorized"
                                                       :status 401
                                                       :detail "Invalid or expired token"}})))))))}))
